package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import lombok.Data;

@Data
public class WarehouseQueryParams {
    private String name;
    private String countryCode;
    private String cityCode;
    private Boolean online;
}
