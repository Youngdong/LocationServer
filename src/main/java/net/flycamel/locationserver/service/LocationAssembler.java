package net.flycamel.locationserver.service;

import io.grpc.locationservice.LocationServiceOuterClass.CommonLocationInfo;
import io.grpc.locationservice.LocationServiceOuterClass.LocationHistoryInfo;
import io.grpc.locationservice.LocationServiceOuterClass.SearchResultInfo;
import net.flycamel.locationserver.domain.LocationData;

public class LocationAssembler {
    public static CommonLocationInfo toCommonLocationInfo(LocationData data) {
        return CommonLocationInfo.newBuilder()
                .setId(data.getId())
                .setLatitude(data.getLatitude())
                .setLongitude(data.getLongitude())
                .setLastUpdatedEpoch(data.getUpdateTimeEpoch())
                .build();
    }

    public static LocationHistoryInfo toLocationHistoryInfo(LocationData data) {
        return LocationHistoryInfo.newBuilder()
                .setLatitude(data.getLatitude())
                .setLongitude(data.getLongitude())
                .setTime(data.getUpdateTimeEpoch())
                .build();
    }

    public static SearchResultInfo toSearchResultInfo(LocationData data, double radius) {
        return SearchResultInfo.newBuilder()
                .setId(data.getId())
                .setLatitude(data.getLatitude())
                .setLongitude(data.getLongitude())
                .setRadius(radius)
                .setLastUpdatedEpoch(data.getUpdateTimeEpoch())
                .build();
    }
}
