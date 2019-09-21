package swarm.enhancer.foursquare;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface Foursquare {

    String API_URL = "https://api.foursquare.com/v2/";
    String API_VERSION = "20190921";

    @GET("venues/search?v=" + API_VERSION)
    Call<VenuesResponse> searchVenues(@Query("ll") String ll, @Query("oauth_token") String accessToken);

    @POST("checkins/add?v=" + API_VERSION)
    Call<Void> createCheckIn(@Query("venueId") String venueId, @Query("shout") String shout, @Query("oauth_token") String accessToken);

    @GET("users/self/checkins?v=" + API_VERSION)
    Call<CheckInsResponse> getMyCheckIns(@Query("limit") int limit, @Query("oauth_token") String accessToken);
}
