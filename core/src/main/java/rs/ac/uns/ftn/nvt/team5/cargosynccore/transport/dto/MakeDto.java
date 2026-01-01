package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make}
 */
@Data
public class MakeDto implements Serializable {

    @NotBlank
    private String wmi;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String name;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<ModelDto> models;

}