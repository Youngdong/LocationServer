package net.flycamel.locationserver.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LocationData implements Indexable<String, Long> {
    String id;
    long updateTimeEpoch;

    double latitude;
    double longitude;

    public LocationData() {
        ;
    }

    @Override
    @JsonIgnore
    public String getKey() {
        return id;
    }

    @Override
    @JsonIgnore
    public Long getSecondKey() {
        return updateTimeEpoch;
    }
}
