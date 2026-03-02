package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record WarehouseHeartbeatMessage(
        @JsonProperty("warehouse_id") UUID warehouseId,
        String type,
        Instant timestamp
) {}
