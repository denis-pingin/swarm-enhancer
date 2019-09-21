package swarm.enhancer.foursquare;

public class Venue {
    public final String id;
    public final String name;
    public final Location location;

    public Venue(String id, String name, Location location) {
        this.id = id;
        this.name = name;
        this.location = location;
    }
}
