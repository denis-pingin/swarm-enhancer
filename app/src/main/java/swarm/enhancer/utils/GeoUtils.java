package swarm.enhancer.utils;

public class GeoUtils {
    public static double distance(double lat1, double lon1, double lat2, double lon2, Unit unit) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        }
        else {
            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515;
            switch (unit) {
                case KILOMETERS:
                    return dist * 1.609344;
                case MILES:
                    return dist * 1.609344;
                case NAUTICAL_MILES:
                    return dist * 0.8684;
                default:
                    throw new RuntimeException(String.format("Invalid unit: %s", unit));
            }
        }
    }

    public enum Unit {
        KILOMETERS,
        MILES,
        NAUTICAL_MILES
    }
}
