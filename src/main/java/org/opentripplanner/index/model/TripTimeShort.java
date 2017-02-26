package org.opentripplanner.index.model;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import com.beust.jcommander.internal.Lists;

public class TripTimeShort {

    public static final int UNDEFINED = -1;
    public AgencyAndId stopId;
    public int stopIndex;
    public int stopCount;
    public int scheduledArrival = UNDEFINED ;
    public int scheduledDeparture = UNDEFINED ;
    public int realtimeArrival = UNDEFINED ;
    public int realtimeDeparture = UNDEFINED ;
    public int arrivalDelay = UNDEFINED ;
    public int departureDelay = UNDEFINED ;
    public boolean timepoint = false;
    public boolean realtime = false;
    public RealTimeState realtimeState = RealTimeState.SCHEDULED ;
    public long serviceDay;
    public AgencyAndId tripId;
    public String blockId;
    public String headsign;

    /**
     * This is stop-specific, so the index i is a stop index, not a hop index.
     */
    public TripTimeShort(TripTimes tt, int i, Stop stop) {
        stopId = stop.getId();
        stopIndex          = i;
        stopCount          = tt.getNumStops();
        scheduledArrival   = tt.getScheduledArrivalTime(i);
        realtimeArrival    = tt.getArrivalTime(i);
        arrivalDelay       = tt.getArrivalDelay(i);
        scheduledDeparture = tt.getScheduledDepartureTime(i);
        realtimeDeparture  = tt.getDepartureTime(i);
        departureDelay     = tt.getDepartureDelay(i);
        timepoint          = tt.isTimepoint(i);
        realtime           = !tt.isScheduled();
        realtimeState      = tt.getRealTimeState();
        blockId            = tt.trip.getBlockId();
        headsign           = tt.getHeadsign(i);
    }

    public TripTimeShort(TripTimes tt, int i, Stop stop, ServiceDay sd) {
        this(tt, i, stop);
        tripId = tt.trip.getId();
        serviceDay = sd.time(0);
    }

    /**
     * must pass in both table and trip, because tripTimes do not have stops.
     */
    public static List<TripTimeShort> fromTripTimes (Timetable table, Trip trip) {
        TripTimes times = table.getTripTimes(table.getTripIndex(trip.getId()));        
        List<TripTimeShort> out = Lists.newArrayList();
        // one per stop, not one per hop, thus the <= operator
        for (int i = 0; i < times.getNumStops(); ++i) {
            out.add(new TripTimeShort(times, i, table.pattern.getStop(i)));
        }
        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TripTimeShort that = (TripTimeShort) o;

        if (stopIndex != that.stopIndex) return false;
        if (stopCount != that.stopCount) return false;
        if (scheduledArrival != that.scheduledArrival) return false;
        if (scheduledDeparture != that.scheduledDeparture) return false;
        if (realtimeArrival != that.realtimeArrival) return false;
        if (realtimeDeparture != that.realtimeDeparture) return false;
        if (arrivalDelay != that.arrivalDelay) return false;
        if (departureDelay != that.departureDelay) return false;
        if (timepoint != that.timepoint) return false;
        if (realtime != that.realtime) return false;
        if (serviceDay != that.serviceDay) return false;
        if (stopId != null ? !stopId.equals(that.stopId) : that.stopId != null) return false;
        if (realtimeState != that.realtimeState) return false;
        if (tripId != null ? !tripId.equals(that.tripId) : that.tripId != null) return false;
        if (blockId != null ? !blockId.equals(that.blockId) : that.blockId != null) return false;
        return headsign != null ? headsign.equals(that.headsign) : that.headsign == null;
    }

    @Override
    public int hashCode() {
        int result = stopId != null ? stopId.hashCode() : 0;
        result = 31 * result + stopIndex;
        result = 31 * result + stopCount;
        result = 31 * result + scheduledArrival;
        result = 31 * result + scheduledDeparture;
        result = 31 * result + realtimeArrival;
        result = 31 * result + realtimeDeparture;
        result = 31 * result + arrivalDelay;
        result = 31 * result + departureDelay;
        result = 31 * result + (timepoint ? 1 : 0);
        result = 31 * result + (realtime ? 1 : 0);
        result = 31 * result + (realtimeState != null ? realtimeState.hashCode() : 0);
        result = 31 * result + (int) (serviceDay ^ (serviceDay >>> 32));
        result = 31 * result + (tripId != null ? tripId.hashCode() : 0);
        result = 31 * result + (blockId != null ? blockId.hashCode() : 0);
        result = 31 * result + (headsign != null ? headsign.hashCode() : 0);
        return result;
    }
}
