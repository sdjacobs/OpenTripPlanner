package org.opentripplanner.index.model;

import java.util.Collection;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.gtfs.GtfsLibrary;

import com.beust.jcommander.internal.Lists;

public class RouteShort {

    public AgencyAndId id;
    public String shortName;
    public String longName;
    public String mode;
    public String color;
    public String agencyName;

    public RouteShort (Route route) {
        id = route.getId();
        shortName = route.getShortName();
        longName = route.getLongName();
        mode = GtfsLibrary.getTraverseMode(route).toString();
        color = route.getColor();
        agencyName = route.getAgency().getName();
    }

    public static List<RouteShort> list (Collection<Route> in) {
        List<RouteShort> out = Lists.newArrayList();
        for (Route route : in) out.add(new RouteShort(route));
        return out;
    }

    @Override
    public String toString() {
        if (shortName != null)
            return shortName;
        if (longName != null)
            return longName;
        if (id != null)
            return id.toString();
        return "RouteShort";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RouteShort that = (RouteShort) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (shortName != null ? !shortName.equals(that.shortName) : that.shortName != null) return false;
        if (longName != null ? !longName.equals(that.longName) : that.longName != null) return false;
        if (mode != null ? !mode.equals(that.mode) : that.mode != null) return false;
        if (color != null ? !color.equals(that.color) : that.color != null) return false;
        return agencyName != null ? agencyName.equals(that.agencyName) : that.agencyName == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (shortName != null ? shortName.hashCode() : 0);
        result = 31 * result + (longName != null ? longName.hashCode() : 0);
        result = 31 * result + (mode != null ? mode.hashCode() : 0);
        result = 31 * result + (color != null ? color.hashCode() : 0);
        result = 31 * result + (agencyName != null ? agencyName.hashCode() : 0);
        return result;
    }
}
