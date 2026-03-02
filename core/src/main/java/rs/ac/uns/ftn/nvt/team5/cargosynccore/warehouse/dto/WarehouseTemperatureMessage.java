package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WarehouseTemperatureMessage(
        @JsonProperty("warehouse_id") UUID warehouseId,
        String type,
        Instant timestamp,
        List<SectorReading> readings
) {
    public record SectorReading(
            @JsonProperty("sector_id")   String sectorId,
            @JsonProperty("sector_name") String sectorName,
            double temperature
    ) {}
}
