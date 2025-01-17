package net.flycamel.locationserver.domain;

import org.springframework.stereotype.Service;

@Service
public class DistanceServiceImpl implements DistanceService {
    public static final double R = 6372.8;

    @Override
    public double getDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.pow(Math.sin(dLat / 2),2) + Math.pow(Math.sin(dLon / 2),2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return R * c;
    }
}
