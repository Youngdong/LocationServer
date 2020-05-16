package net.flycamel.locationserver.domain;

import io.grpc.locationservice.LocationServiceOuterClass.LocationHistoryInfo;
import io.grpc.locationservice.LocationServiceOuterClass.SearchResultInfo;

import java.util.List;
import java.util.Optional;

public interface LocationDataService {
    LocationData save(LocationData data);

    Optional<LocationData> findLastLocation(String id);

    List<LocationHistoryInfo> findHistory(String id, long startTime, long endTime);

    List<SearchResultInfo> search(double latitude, double longitude, double radius);
}
