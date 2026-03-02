package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.util.Set;

@Schema(description = "Query parameters for filtering vans.")
@Data
public class VanQueryParams {
    @Nullable String q;
    Set<@NotBlank String> make;
    Set<@NotBlank String> model;
    @Positive Integer canCarryKg;
    @Positive Integer tooBigIfCanCarryKg;
    transient Pageable pageable = Pageable.unpaged();
}
