package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Value;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation}
 */
@Value
public class GeoLocationDto implements Serializable {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    BigDecimal longitude;

    String getUri() {
        return "geo:%.6f,%.6f".formatted(latitude.doubleValue(), longitude.doubleValue());
    }

}