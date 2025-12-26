package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

import static lombok.AccessLevel.PROTECTED;

@Data
@NoArgsConstructor(access = PROTECTED)
@Embeddable
public class GeoLocation {

    @NotNull
    @DecimalMin("-90.0")
    @DecimalMax("90.0")
    private BigDecimal latitude;

    @NotNull
    @DecimalMin("-180.0")
    @DecimalMax("180.0")
    private BigDecimal longitude;

    // NOTE: Maybe add altitude

    //region Should've made a dto

    @JsonProperty("uri")
    @Transient
    public String getUri() {
        // uri also supports altitude and precision
        return "geo:%.6f,%.6f".formatted(latitude.doubleValue(), longitude.doubleValue());
    }

    //endregion

}
