package net.flycamel.locationserver.service;

import io.grpc.locationservice.LocationServiceGrpc;
import io.grpc.locationservice.LocationServiceOuterClass.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.flycamel.locationserver.domain.LocationData;
import net.flycamel.locationserver.domain.LocationDataService;
import org.lognet.springboot.grpc.GRpcService;

import java.util.List;

@GRpcService
@Slf4j
public class LocationService extends LocationServiceGrpc.LocationServiceImplBase {

    private final LocationDataService locationDataService;

    public LocationService(LocationDataService locationDataService) {
        this.locationDataService = locationDataService;
    }

    @Override
    public void put(PutRequest request, StreamObserver<CommonLocationInfo> responseObserver) {
        log.info("Server Received : {}", request);

        LocationData data = new LocationData(request.getId(),
                System.currentTimeMillis(),
                request.getLatitude(),
                request.getLongitude());

        data = locationDataService.save(data);

        CommonLocationInfo response = LocationAssembler.toCommonLocationInfo(data);
        log.info("Server Send : {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void get(GetRequest request, StreamObserver<CommonLocationInfo> responseObserver) {
        log.info("Get Received : {}", request);

        CommonLocationInfo response = locationDataService.findLastLocation(request.getId())
                .map(LocationAssembler::toCommonLocationInfo)
                .orElse(null);

        log.info("Server Send : {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void search(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        log.info("Search Received : {}", request);

        List<SearchResultInfo> searchResultInfoList =
                locationDataService.search(request.getLatitude(), request.getLongitude(), request.getRadius());

        SearchResponse response = SearchResponse.newBuilder()
                .setSearchTime(System.currentTimeMillis())
                .setResultCount(searchResultInfoList.size())
                .addAllSearchResultList(searchResultInfoList)
                .build();

        log.info("Search Send : {}", response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void history(HistoryRequest request, StreamObserver<HistoryResponse> responseObserver) {
        log.info("History Received : {}", request);

        List<LocationHistoryInfo> historyInfoList =
                locationDataService.findHistory(request.getId(), request.getStartTime(), request.getEndTime());

        HistoryResponse response = HistoryResponse.newBuilder()
                .setId(request.getId())
                .setResultCount(historyInfoList.size())
                .addAllHistoryList(historyInfoList)
                .build();

        log.info("History Send : {}", response);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
