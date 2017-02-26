package org.opentripplanner.profile;

import com.beust.jcommander.internal.Sets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import org.opentripplanner.index.model.RouteShort;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PatternHop;
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

    public Set<Option> options = Sets.newHashSet();

    /**
     * The constructed response will include all the options that do not use transit,
     * as well as the top N options that do use transit for each access mode.
     *
     * @param allOptions a collection of Options with a mix of all access and egress modes, using transit or not.
     * @param orderBy specifies how the top N transit options will be chosen.
     * @param limit the maximum number of transit options to include in the response per access mode.
     *              zero or negative means no limit.
     */
    public ProfileResponse (Collection<Option> allOptions, Option.SortOrder orderBy, int limit, Graph graph) {
        List<Option> transitOptions = Lists.newArrayList();
        // Always return all non-transit options
        for (Option option : allOptions) {
            if (option.transit == null || option.transit.isEmpty()) options.add(option);
            else transitOptions.add(option);
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
        // Retain the top N transit options for each access mode. Duplicates may be present, but options is a Set.
        for (Collection<Option> singleModeOptions : transitOptionsByAccessMode.asMap().values()) {
            int n = 0;
            for (Option option : singleModeOptions) {
                options.add(option);
                if (limit > 0 && ++n >= limit) break;
            }
        }
        for (Option option : this.options) {
            LOG.info("{} {}", option.stats, option.summary);
        }
    }

    // sort by difference
    // filter out bad results
    // add schedules
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

        Option best = Collections.max(optionsByLongestLeg.values(), (s,t) -> s.stats.avg - t.stats.avg);

        while (ret.size() != options.size()) {
            for (Iterator<Option> iter : optIter) {
                if (iter.hasNext()) {
                    Option opt = iter.next();
                    addSchedules(option);
                    if (opt.stats.avg < best.stats.avg * 2 || !filterBadResults)
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
}
