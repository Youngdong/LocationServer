package net.flycamel.locationserver.domain;

import io.grpc.locationservice.LocationServiceOuterClass.LocationHistoryInfo;
import io.grpc.locationservice.LocationServiceOuterClass.SearchResultInfo;
import lombok.extern.slf4j.Slf4j;
import net.flycamel.locationserver.service.LocationAssembler;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocationDataServiceImpl implements LocationDataService {
    private final static Comparator<LocationHistoryInfo> TIME_COMPARATOR
            = Comparator.comparingLong(LocationHistoryInfo::getTime);

    private final static Comparator<SearchResultInfo> DISTANCE_COMPARATOR
            = Comparator.comparingDouble(SearchResultInfo::getRadius);

    private final StorageRepository storageRepository;
    private final DistanceService distanceService;

    public LocationDataServiceImpl(StorageRepository storageRepository, DistanceService distanceService) {
        this.storageRepository = storageRepository;
        this.distanceService = distanceService;
    }

    @Override
    public LocationData save(LocationData data) {
        return storageRepository.save(data);
    }

    @Override
    public Optional<LocationData> findLastLocation(String id) {
        return storageRepository.findLastLocation(id);
    }

    @Override
    public List<LocationHistoryInfo> findHistory(String id, long startTime, long endTime) {
        return storageRepository.findHistory(id, startTime, endTime)
                .stream()
                .map(LocationAssembler::toLocationHistoryInfo)
                .sorted(TIME_COMPARATOR)
                .collect(Collectors.toList());
    }

    @Override
    public List<SearchResultInfo> search(double latitude, double longitude, double radius) {
        return storageRepository.getAllLastLocation()
                .parallelStream()
                .map(locationData -> {
                    double distance = distanceService.getDistance(latitude, longitude,
                            locationData.getLatitude(), locationData.getLongitude());

                    log.debug("Distance : [{},{}],[{},{}],{}", latitude, longitude,
                            locationData.getLatitude(), locationData.getLongitude(), distance);

                    return LocationAssembler.toSearchResultInfo(locationData, distance);
                })
                .filter(searchResultInfo -> searchResultInfo.getRadius() <= radius)
                .sorted(DISTANCE_COMPARATOR)
                .collect(Collectors.toList());
    }
}
