package org.opentripplanner.index.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.edgetype.TripPattern;

public class StopPairSchedule {
    public TripTimeShort orig;
    public TripTimeShort dest;
    public PatternShort pattern;
    public RouteShort route;
    public AgencyAndId tripId;

    public StopPairSchedule(TripPattern pattern, TripTimeShort origin, TripTimeShort destination) {
        this.orig = origin;
        this.dest = destination;
        this.pattern = new PatternShort(pattern);
        this.route = new RouteShort(pattern.route);
        this.tripId = origin.tripId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StopPairSchedule that = (StopPairSchedule) o;

        if (!orig.equals(that.orig)) return false;
        if (!dest.equals(that.dest)) return false;
        if (!pattern.id.equals(that.pattern.id)) return false;
        if (!route.id.equals(that.route.id)) return false;
        return (tripId.equals(that.tripId));
    }

    @Override
    public int hashCode() {
        int result = orig.hashCode();
        result = 31 * result + dest.hashCode();
        result = 31 * result + pattern.id.hashCode();
        result = 31 * result + route.id.hashCode();
        result = 31 * result + tripId.hashCode();
        return result;
    }
}
