package swarm.enhancer.foursquare;

import java.util.List;

public class CheckInsResponse {
    public final Response response;

    public CheckInsResponse(Response response) {
        this.response = response;
    }

    public class CheckInItems {
        public final List<CheckIn> items;

        public CheckInItems(List<CheckIn> items) {
            this.items = items;
        }
    }

    public class Response {
        public final CheckInItems checkins;

        public Response(CheckInItems checkins) {
            this.checkins = checkins;
        }
    }
}
