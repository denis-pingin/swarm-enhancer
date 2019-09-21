package swarm.enhancer.foursquare;

public class CheckIn {
    public final String id;
    public final Venue venue;
    public final CheckInSource source;

    public CheckIn(String id, Venue venue, CheckInSource source) {
        this.id = id;
        this.venue = venue;
        this.source = source;
    }

    public class CheckInSource {
        public final String name;
        public final String url;

        public CheckInSource(String name, String url) {
            this.name = name;
            this.url = url;
        }
    }
}
