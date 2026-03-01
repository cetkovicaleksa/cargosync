package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.ClimateZone;

import java.time.Instant;
import java.util.UUID;

@Data
public class WarehouseSectorDto {
    private UUID id;
    private String name;
    private ClimateZone climateZone;
    private Double lastTemperature;
    private Instant lastTemperatureAt;
}
