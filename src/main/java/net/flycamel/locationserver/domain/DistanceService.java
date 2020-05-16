package net.flycamel.locationserver.domain;

public interface DistanceService {
    double getDistance(double lat1, double lon1, double lat2, double lon2);
}
