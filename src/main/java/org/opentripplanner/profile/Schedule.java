package org.opentripplanner.profile;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.index.model.StopPairSchedule;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Schedule {

    // schedule is defined JUST by the sequence of trip IDs.
    private List<StopPairSchedule> schedule;
    private List<AgencyAndId> tripIds;

    private int accessTime;
    private int egressTime;

    public Schedule(List<StopPairSchedule> schedule, int accessTime, int egressTime) {
        this.schedule = schedule;
        tripIds = schedule.stream().map(s -> s.tripId).collect(Collectors.toList());
        this.accessTime = accessTime;
        this.egressTime = egressTime;
    }

    public static Collection<Schedule> fromCollection(Collection<List<StopPairSchedule>> col, int accessTime, int egressTime) {
        return col.stream().map(x -> new Schedule(x, accessTime, egressTime)).collect(Collectors.toSet());
    }

    public List<StopPairSchedule> getSchedule() {
        return schedule;
    }

    public int hashCode() {
        return tripIds.hashCode();
    }

    public boolean equals(Object o) {
        if (o == null || !o.getClass().equals(this.getClass()))
            return false;
        Schedule that = (Schedule) o;
        return that.tripIds.equals(tripIds);
    }

    public int getAccessTime() {
        return accessTime;
    }

    public int getEgressTime() {
        return egressTime;
    }
}
