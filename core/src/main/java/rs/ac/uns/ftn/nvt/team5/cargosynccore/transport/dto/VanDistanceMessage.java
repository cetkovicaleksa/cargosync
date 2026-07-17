package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;

import java.time.Instant;

public record VanDistanceMessage(
        @JsonProperty("van_id")
        @JsonAlias({"vin", "vehicle_id"})
        String vanId,
        String type,
        Instant timestamp,
        GeoLocationDto location,
        @JsonAlias({"distance", "distance_meters", "distanceMeters", "covered_distance", "coveredDistance"})
        Double distance
) {}