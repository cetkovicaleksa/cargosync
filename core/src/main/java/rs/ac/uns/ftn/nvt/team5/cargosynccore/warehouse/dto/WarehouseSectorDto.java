package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class WarehouseSectorDto {
    private UUID id;
    private String name;
    private String climateZone;
    private Double lastTemperature;
    private Instant lastTemperatureAt;
}
