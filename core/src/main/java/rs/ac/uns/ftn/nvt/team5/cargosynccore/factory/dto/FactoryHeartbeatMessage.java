package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record FactoryHeartbeatMessage(
        @JsonProperty("factory_id") UUID factoryId,
        String type,
        Instant timestamp
) {}
