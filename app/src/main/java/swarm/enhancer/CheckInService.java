package swarm.enhancer;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.location.Location;
import android.os.PersistableBundle;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.http.HttpStatus;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import swarm.enhancer.foursquare.CheckIn;
import swarm.enhancer.foursquare.CheckInsResponse;
import swarm.enhancer.foursquare.Foursquare;
import swarm.enhancer.foursquare.Venue;
import swarm.enhancer.foursquare.VenuesResponse;
import swarm.enhancer.utils.GeoUtils;
import swarm.enhancer.utils.Log;

public class CheckInService extends JobService {

    public static final int JOB_ID = 0;

    public static boolean SHOW_INFO_TOASTS = true;
    public static boolean SHOW_DEBUG_TOASTS = false;

//    public static final long REFRESH_PERIOD_MILLIS = 15 * 1000;
    public static final long REFRESH_PERIOD_MILLIS = 4 * 60 * 60 * 1000;
    public static final double LOCAL_RADIUS = 5;
    public static final String CHECK_IN_SHOUT = "Automated check-in by Swarm Enhancer";

    private static Foursquare foursquare;

    private FusedLocationProviderClient fusedLocationClient;
    private JobScheduler jobScheduler;
    private JobInfo jobInfo;

    private static CheckInService instance;

    public static CheckInService get() {
        if (instance == null){
            instance = new CheckInService();
        }
        return instance;
    }

    public void start(Context context, String token) {
        scheduleJob(context, token, swarm.enhancer.foursquare.Location.invalid(), 0);
    }

    private void scheduleJob(Context context, String token, swarm.enhancer.foursquare.Location location, long interval) {

        // Get info on the previously scheduled job
        JobInfo jobInfo = getJobInfo(context);

        // Set job parameters
        PersistableBundle bundle = new PersistableBundle();
        bundle.putString("token", token);
        if (jobInfo == null) {
            bundle.putDouble("lat", location.lat);
            bundle.putDouble("lng", location.lng);
        } else {
            bundle.putDouble("lat", jobInfo.getExtras().getDouble("lat"));
            bundle.putDouble("lng", jobInfo.getExtras().getDouble("lng"));
        }

        scheduleJobWithParameters(context, bundle, interval);
    }

    private void scheduleJobWithParameters(Context context, PersistableBundle bundle, long interval) {
        // Build job info
        jobInfo = new JobInfo.Builder(CheckInService.JOB_ID, new ComponentName(context, CheckInService.class))
                .setMinimumLatency(interval)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setExtras(bundle)
                .build();

        // Schedule job and check result
        int result = getJobScheduler(this).schedule(jobInfo);
        if (result == JobScheduler.RESULT_FAILURE) {
            Log.e(CheckInService.class, this, String.format(Locale.US, "Failed to schedule service job: %d", result), true);
            throw new RuntimeException("Failed to schedule job");
        } else {
            Log.d(CheckInService.class, this, String.format(Locale.US, "Service job scheduled in %d ms", interval), SHOW_DEBUG_TOASTS);
        }
    }

    private void updateJobParameterLocation(Context context, swarm.enhancer.foursquare.Location location) {
        // Get job info
        JobInfo jobInfo = getJobInfo(context);
        if (jobInfo == null) {
            Log.e(CheckInService.class, context, "Pending job not found", true);
            throw new RuntimeException("Pending job not found");
        }

        PersistableBundle bundle = new PersistableBundle();
        bundle.putDouble("lat", location.lat);
        bundle.putDouble("lng", location.lng);
        bundle.putString("token", jobInfo.getExtras().getString("token"));

        scheduleJobWithParameters(context, bundle, REFRESH_PERIOD_MILLIS);
    }

    private JobScheduler getJobScheduler(Context context) {
        if (jobScheduler == null) {
            jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(CheckInService.class, context, "Failed to retrieve JobScheduler", true);
                throw new RuntimeException("Failed to retrieve JobScheduler");
            }
        }
        return jobScheduler;
    }

    private JobInfo getJobInfo(Context context) {
        if (jobInfo == null) {
            jobInfo = getJobScheduler(context).getPendingJob(CheckInService.JOB_ID);
        }
        return jobInfo;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Retrieve location fused client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        // Get job parameters
        final String token = params.getExtras().getString("token");
        final swarm.enhancer.foursquare.Location previousLocation = new swarm.enhancer.foursquare.Location(
                params.getExtras().getDouble("lat", Double.NaN),
                params.getExtras().getDouble("lng", Double.NaN));

        // Perform service work
        doServiceWork(token, previousLocation);

        // Finish job
        jobFinished(params, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    private Foursquare foursquare() {
        if (foursquare == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(Foursquare.API_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            foursquare = retrofit.create(Foursquare.class);
        }
        return foursquare;
    }

    private void doServiceWork(final String token, final swarm.enhancer.foursquare.Location previousLocation) {
        Log.d(CheckInService.class, this, String.format(Locale.US, "Starting service execution with previous location: %f,%f", previousLocation.lat, previousLocation.lng), SHOW_DEBUG_TOASTS);

        // Retrieve last known location
        fusedLocationClient
                .getLastLocation()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(CheckInService.class, CheckInService.this, e.getMessage(), true, e);

                        // Schedule the next service invocation
                        scheduleJob(CheckInService.this, token, previousLocation, REFRESH_PERIOD_MILLIS);
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Check if current location is available
                        swarm.enhancer.foursquare.Location currentLocation = previousLocation;
                        if (location == null) {
                            // Current location is not available
                            Log.w(CheckInService.class, CheckInService.this, "Location service responded with invalid location", true);
                        } else {
                            Log.d(CheckInService.class, CheckInService.this, String.format("Last location: %f, %f", location.getLatitude(), location.getLongitude()), SHOW_DEBUG_TOASTS);

                            // Set current location
                            currentLocation = new swarm.enhancer.foursquare.Location(location.getLatitude(), location.getLongitude());

                            // Handle current location
                            handleCurrentLocation(currentLocation, previousLocation, token);
                        }

                        // Schedule the next service invocation
                        scheduleJob(CheckInService.this, token, previousLocation, REFRESH_PERIOD_MILLIS);
                    }
                });
    }

    private void handleCurrentLocation(swarm.enhancer.foursquare.Location currentLocation, swarm.enhancer.foursquare.Location previousLocation, final String token) {
        // Check if our location has changed significantly compared to the previous invocation
        boolean locationChangedSignificantly = !isWithinLocalRadius(currentLocation, previousLocation);

        // Do nothing if we did not move far enough
        if (!locationChangedSignificantly) {
            Log.i(CheckInService.class, CheckInService.this, "Check in not required, similar location", SHOW_INFO_TOASTS);
            return;
        }

        // Search for venues near our current location
        final String ll = String.format(Locale.US, "%f,%f", currentLocation.lat, currentLocation.lng);
        foursquare().searchVenues(ll, token).enqueue(new Callback<VenuesResponse>() {
            @Override
            public void onResponse(Call<VenuesResponse> call, Response<VenuesResponse> response) {
                // Check response status
                if (response.code() != HttpStatus.SC_OK) {
                    Log.e(CheckInService.class, CheckInService.this, String.format("Failed to find venues: %s, %s", response.code(), response.message()), true);
                    return;
                }

                // Process venues response
                VenuesResponse venuesResponse = response.body();
                List<Venue> venues = venuesResponse.response.venues;
                if (venues.size() < 1) {
                    Log.w(CheckInService.class, CheckInService.this, String.format("No venues found for ll: %s", ll), true);
                    return;
                }

                // Take the first available venue
                final Venue venue = venues.get(0);
                Log.d(CheckInService.class, CheckInService.this, String.format("Found venue: %s, %s", venue.id, venue.name), SHOW_DEBUG_TOASTS);

                // Handle venue
                handleVenue(venue, token);
            }

            @Override
            public void onFailure(Call<VenuesResponse> call, Throwable t) {
                Log.e(CheckInService.class, CheckInService.this, String.format("Failed to find venues: %s", t.getMessage()), false, t);
            }
        });
    }

    private void handleVenue(final Venue venue, final String token) {
        // Retrieve my current check-ins
        foursquare().getMyCheckIns(1, token).enqueue(new Callback<CheckInsResponse>() {
            @Override
            public void onResponse(Call<CheckInsResponse> call, Response<CheckInsResponse> response) {
                // Check response status
                if (response.code() != HttpStatus.SC_OK) {
                    Log.e(CheckInService.class, CheckInService.this, String.format("Failed to get current check in: %s, %s", response.code(), response.message()), true);
                    return;
                }

                // Retrieve check-ins
                CheckInsResponse checkInsResponse = response.body();
                List<CheckIn> checkIns = checkInsResponse.response.checkins.items;

                // Determine if a new check-in is required
                boolean doCheckIn = isCheckInRequired(checkIns, venue);

                // Check in if required
                if (doCheckIn) {
                    checkInToVenue(venue, token);
                } else {
                    Log.i(CheckInService.class, CheckInService.this, "Check in not required, venue is close to the previous one", SHOW_INFO_TOASTS);
                }
            }

            @Override
            public void onFailure(Call<CheckInsResponse> call, Throwable t) {
                Log.e(CheckInService.class, CheckInService.this, String.format("Failed to check in: %s", t.getMessage()), true, t);
            }
        });
    }

    private boolean isCheckInRequired(List<CheckIn> checkIns, Venue venue) {
        boolean doCheckIn = true;

        // Check the amount of current check-ins. Check in if none found
        if (checkIns.size() > 0) {
            // Check if the most recent check-in has venue
            CheckIn checkIn = checkIns.get(0);
            if (checkIn.venue != null) {
                // Check if we found the same venue that we are already checked-in to
                if (venue.id.equals(checkIn.venue.id)) {
                    doCheckIn = false;
                } else {
                    // Check if the venue that we have found is far enough from the current venue
                    doCheckIn = !isWithinLocalRadius(checkIn.venue, venue);
                }

                updateJobParameterLocation(CheckInService.this, venue.location);
            }
        }
        return doCheckIn;
    }

    private void checkInToVenue(final Venue venue, final String token) {
        foursquare().createCheckIn(venue.id, CHECK_IN_SHOUT, token).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {

                if (response.code() != HttpStatus.SC_OK) {
                    Log.e(CheckInService.class, CheckInService.this, String.format("Failed to check in: %s, %s", response.code(), response.message()), true);
                } else {
                    Log.i(CheckInService.class, CheckInService.this, String.format("Successfully checked in to %s", venue.name), true);

                    updateJobParameterLocation(CheckInService.this, venue.location);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(CheckInService.class, CheckInService.this, String.format("Failed to check in: %s", t.getMessage()), true, t);
            }
        });
    }

    private boolean isWithinLocalRadius(swarm.enhancer.foursquare.Location location1, swarm.enhancer.foursquare.Location location2) {
        if (location1 == null || location2 == null || !location1.isValid() || !location2.isValid()) {
            return false;
        }

        double distance = GeoUtils.distance(location1.lat, location1.lng, location2.lat, location2.lng, GeoUtils.Unit.KILOMETERS);
        Log.d(CheckInService.class, CheckInService.this, String.format(Locale.US, "Distance between locations is %f km", distance), SHOW_DEBUG_TOASTS);

        return distance < LOCAL_RADIUS;
    }

    private boolean isWithinLocalRadius(Venue venue1, Venue venue2) {
        if (venue1.location == null || venue2.location == null || !venue1.location.isValid() || !venue2.location.isValid()) {
            return false;
        }

        double distance = GeoUtils.distance(venue1.location.lat, venue1.location.lng, venue2.location.lat, venue2.location.lng, GeoUtils.Unit.KILOMETERS);
        Log.d(CheckInService.class, CheckInService.this, String.format(Locale.US, "Distance between venues %s and %s is %f km", venue1.name, venue2.name, distance), SHOW_DEBUG_TOASTS);

        return distance < LOCAL_RADIUS;
    }
}
