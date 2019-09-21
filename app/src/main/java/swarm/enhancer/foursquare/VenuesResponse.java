package swarm.enhancer.foursquare;

import java.util.List;

public class VenuesResponse {
    public final Response response;

    public VenuesResponse(Response response) {
        this.response = response;
    }

    public class Response {
        public final List<Venue> venues;

        public Response(List<Venue> venues) {
            this.venues = venues;
        }
    }
}
