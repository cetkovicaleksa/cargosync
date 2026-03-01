package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CityDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
public class WarehouseDto {
    private UUID id;
    private String name;
    private String address;
    private CityDto city;
    private GeoLocationDto location;
    private List<String> images;
    private boolean online;
    private Instant lastHeartbeat;
    private List<WarehouseSectorDto> sectors;
}
