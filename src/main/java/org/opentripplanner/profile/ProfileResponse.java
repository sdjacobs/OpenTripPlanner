package org.opentripplanner.profile;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.index.model.StopPairSchedule;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

// Jackson will serialize fields with getters, or @JsonProperty annotations.
public class ProfileResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ProfileResponse.class);

    private static final boolean filterBadResults = true;
    private static final int minTransferTime = 5 * 60;
    private static double filter = 1.5;

    private static final Comparator<List<StopPairSchedule>> EARLIEST_SCHED = (s, t) ->
                    s.get(0).orig.realtimeDeparture - t.get(0).orig.realtimeDeparture;

    public Set<Option> options = Sets.newHashSet();

    public Set<Schedule> schedules = Sets.newHashSet();

    /**
     * The constructed response will include all the options that do not use transit,
     * as well as the top N options that do use transit for each access mode.
     *
     * @param allOptions a collection of Options with a mix of all access and egress modes, using transit or not.
     * @param orderBy specifies how the top N transit options will be chosen.
     * @param limit the maximum number of transit options to include in the response per access mode.
     *              zero or negative means no limit.
     */
    public ProfileResponse (Collection<Option> allOptions, Option.SortOrder orderBy, int limit, Graph graph, Date date) {
        List<Option> transitOptions = Lists.newArrayList();
        // Always return all non-transit options
        for (Option option : allOptions) {
            if (option.transit == null || option.transit.isEmpty()) options.add(option);
            else
                transitOptions.add(option);

        }
        // Order all transit options by the specified method
        if (!orderBy.equals(Option.SortOrder.DIFFERENCE)) {
            Comparator<Option> c;
            switch (orderBy) {
                case MAX:
                    c = new Option.MaxComparator();
                    break;
                case AVG:
                    c = new Option.AvgComparator();
                    break;
                case MIN:
                default:
                    c = new Option.MinComparator();
            }
            Collections.sort(transitOptions, c);
        } else {
            transitOptions = sortByDifference(transitOptions, graph);
        }
        // Group options by access mode, retaining ordering.
        // ListMultimap can hold duplicate key-value pairs and maintains the insertion ordering of values for a given key.
        // TODO update this to also use the egress mode in the key, and to consider the qualifiers on the modes
        ListMultimap<TraverseMode, Option> transitOptionsByAccessMode = ArrayListMultimap.create();
        for (Option option : transitOptions) {
            for (StreetSegment segment : option.access) {
                transitOptionsByAccessMode.put(segment.mode.mode, option);
            }
        }
        Option best = Collections.min(transitOptions, (s,t) -> s.stats.avg - t.stats.avg);
        // Retain the top N transit options for each access mode. Duplicates may be present, but options is a Set.
        for (Collection<Option> singleModeOptions : transitOptionsByAccessMode.asMap().values()) {
            int n = 0;
            for (Option option : singleModeOptions) {
                if (okOption(option, best)) {
                    options.add(option);
                    if (limit > 0 && ++n >= limit) break;
                }
            }
        }
        for (Option option : this.options) {
            LOG.info("{} {}", option.stats, option.summary);
        }
    }

    // sort by difference
    // filter out bad results
    private static List<Option> sortByDifference(List<Option> options, Graph graph) {

        SortedSetMultimap<String, Option> optionsByLongestLeg = TreeMultimap.create(String::compareTo,
                (a, b) -> a.stats.avg - b.stats.avg);

        for (Option o : options) {
            Segment longestLeg = Collections.max(o.transit, (s, t) -> (int) (distanceForSegment(s, graph) - distanceForSegment(t, graph)));
            String legKey = longestLeg.routes.stream().map(t -> t.id.toString()).min(String::compareTo).get();
            optionsByLongestLeg.put(legKey, o);
        }

        List<Option> ret = Lists.newArrayList();
        Set<Map.Entry<String, Collection<Option>>> opts = optionsByLongestLeg.asMap().entrySet();
        Set<Iterator<Option>> optIter = opts.stream().map(e -> e.getValue().iterator()).collect(Collectors.toSet());

        Option best = Collections.max(options, (s,t) -> s.stats.avg - t.stats.avg);

        while (ret.size() != options.size()) {
            for (Iterator<Option> iter : optIter) {
                if (iter.hasNext()) {
                    Option opt = iter.next();
                    if (opt.stats.avg < best.stats.avg * 1.5 || !filterBadResults)
                        ret.add(opt);
                }
            }
        }

        return ret;
    }

    private static double distanceForSegment(Segment segment, Graph graph) {
        double sum = 0;
        double tot = 0;
        for (Segment.SegmentPattern pat : segment.segmentPatterns) {
            TripPattern pattern = graph.index.patternForId.get(pat.patternId);
            double distance = 0;
            for (int i = pat.fromIndex; i < pat.toIndex; i++)
                distance += pattern.hopEdges[i].getDistance();
            sum += distance;
            tot++;
        }
        return sum/tot;
    }

    public void addTransitTimes(Graph graph, Date date) {
        for (Option option : options) {
            if (option.transit != null) {
                int access = 0, egress = 0;
                for (StreetSegment seg : option.access)
                    access += seg.time;
                for (StreetSegment seg : option.egress)
                    egress += seg.time;
                schedules.addAll(Schedule.fromCollection(addSchedules(option, graph, date), access, egress));
            }
        }
    }

    private List<List<StopPairSchedule>> addSchedules(Option option, Graph graph, Date date) {

        // schedules, ordered by start time
        SortedSet<List<StopPairSchedule>> schedules = new TreeSet<>(EARLIEST_SCHED);

        // iterate through the segments in reverse order creating candidates
        Iterator<Segment> iter = Lists.reverse(option.transit).iterator();
        while (iter.hasNext()) {

            Segment seg = iter.next();
            List<StopPairSchedule> theseTimes = allTransitTimes(graph, seg, date);
            if (theseTimes.isEmpty()) {
                LOG.info("found no times for routes={}, day={}", seg.routes, date);
                return Collections.emptyList();
            }
            if (schedules.isEmpty()) {
                for (StopPairSchedule s : theseTimes) {
                    List<StopPairSchedule> sched = new ArrayList<>();
                    sched.add(s);
                    schedules.add(sched);
                }
                continue;
            }
            // nonempty.
            SortedSet<List<StopPairSchedule>> schedulesNextIteration = new TreeSet<>(EARLIEST_SCHED);
            Iterator<List<StopPairSchedule>> candidates = schedules.iterator();

            List<StopPairSchedule> candidateSchedule = null;
            for (int i = 0; i < theseTimes.size(); i++) {
                StopPairSchedule time = theseTimes.get(i);
                // find first appropriate schedule
                while(candidates.hasNext() && candidateSchedule == null) {
                    candidateSchedule = candidates.next();
                    StopPairSchedule next = candidateSchedule.get(0);
                    if (time.dest.realtimeArrival + minTransferTime < next.orig.realtimeDeparture)
                        break; // found it
                    else
                        candidateSchedule = null;
                }
                if (candidateSchedule == null)
                    continue;
                // check if next time works.
                if (i < theseTimes.size() - 1) {
                    StopPairSchedule nextTime = theseTimes.get(i+1);
                    StopPairSchedule next = candidateSchedule.get(0);
                    if (nextTime.dest.realtimeArrival + minTransferTime < next.orig.realtimeDeparture)
                        continue; // skip this iteration (but hold on to candidateSchedule)
                }

                // we found a good schedule and the next time isn't any better
                candidateSchedule.add(0, time);
                schedulesNextIteration.add(candidateSchedule);
                candidateSchedule = null;
            }
            schedules = schedulesNextIteration;
        }

        return new ArrayList<List<StopPairSchedule>>(schedules);
    }


    private List<StopPairSchedule> allTransitTimes(Graph graph, Segment leg, Date date) {
        long startTime = date.getTime()/1000;
        int timeRange = 24 * 60 * 60; // one day
        List<StopPairSchedule> schedule = new ArrayList<>();
        for (Segment.SegmentPattern pattern : leg.segmentPatterns) {
            TripPattern tripPattern = graph.index.patternForId.get(pattern.patternId);
            List<StopPairSchedule> s =
                    graph.index.stopTimesForPattern(tripPattern, pattern.fromIndex, pattern.toIndex, startTime, timeRange);
            schedule.addAll(s);
        }
        schedule.sort((x, y) -> x.orig.realtimeDeparture - y.orig.realtimeDeparture);
        return schedule;
    }

    private static boolean trivialOption(Option o, Option best) {
        if (o.transit == null)
            return false;
        // o is trivial if it has more transfers than best, but starts with same route
        if (o.transit.size() > best.transit.size()) {
            for (RouteShort route : o.transit.get(0).routes) {
                if (best.transit.get(0).routes.contains(route)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean okOption(Option o, Option best) {
        if (!filterBadResults)
            return true;
        if (trivialOption(o, best))
            return false;
        if (o.stats.avg > filter * best.stats.avg)
            return false;
        if (o.transit.size() >= 3 * best.transit.size())
            return false;
        return true;
    }
}
