package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UpdateWarehouseForm {
    private String name;
    private String address;
    private String countryCode;
    private String cityCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private List<MultipartFile> images;
    private List<String> removeImages;
    // JSON-encoded list of sector definitions, sent as a single JSON string field
    private String sectors;
}
