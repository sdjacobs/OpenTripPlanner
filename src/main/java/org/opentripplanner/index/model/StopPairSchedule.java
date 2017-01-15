package org.opentripplanner.index.model;

import com.google.common.collect.Lists;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.List;

public class StopPairSchedule {
    public TripTimeShort orig;
    public TripTimeShort dest;
    public PatternShort pattern;
    public RouteShort route;

    public StopPairSchedule(TripPattern pattern, TripTimeShort origin, TripTimeShort destination) {
        this.orig = origin;
        this.dest = destination;
        this.pattern = new PatternShort(pattern);
        this.route = new RouteShort(pattern.route);
    }
}
