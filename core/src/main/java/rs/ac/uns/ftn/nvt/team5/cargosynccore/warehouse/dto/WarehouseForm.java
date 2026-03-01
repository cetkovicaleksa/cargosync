package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class WarehouseForm {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotNull
    private String countryCode;

    @NotNull
    private String cityCode;

    private BigDecimal latitude;

    private BigDecimal longitude;

    private List<MultipartFile> images = new ArrayList<>();

    // JSON-encoded list of sector definitions, sent as a form field
    private List<SectorForm> sectors = new ArrayList<>();

    @Data
    public static class SectorForm {
        @NotBlank
        private String name;
        private String climateZone;
    }
}
