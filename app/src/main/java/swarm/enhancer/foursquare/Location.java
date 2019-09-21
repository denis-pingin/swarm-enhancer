package swarm.enhancer.foursquare;

public class Location {
    public final Double lat;
    public final Double lng;

    public Location(Double lat, Double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public static Location invalid() {
        return new Location(Double.NaN, Double.NaN);
    }

    public boolean isValid() {
        return lat != null &&
                lng != null &&
                !lat.isNaN() &&
                !lng.isNaN();
    }
}
