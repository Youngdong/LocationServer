package net.flycamel.locationserver.domain;

import java.util.List;
import java.util.Optional;

public interface StorageRepository {
    LocationData save(LocationData entity);

    Optional<LocationData> findLastLocation(String key);

    List<LocationData> findHistory(String key, long startTime, long endTime);

    List<LocationData> getAllLastLocation();
}
