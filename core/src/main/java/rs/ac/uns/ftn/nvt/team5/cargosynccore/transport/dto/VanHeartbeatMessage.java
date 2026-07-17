package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record VanHeartbeatMessage(
        @JsonProperty("van_id")
        @JsonAlias({"vin", "vehicle_id"})
        String vanId,
        String type,
        Instant timestamp
) {}