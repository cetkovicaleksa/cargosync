package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;

import java.io.Serializable;


/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model}
 */
@Data
public class ModelDto implements Serializable {

    @NotBlank
    private String wmi;

    @NotBlank
    private String vd;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String name;

    @JsonIgnore
    public Model.ModelPK getId() {
        return new Model.ModelPK(wmi, vd);
    }

    @JsonIgnore
    public void setId(Model.ModelPK id) {
        wmi = id.wmi();
        vd = id.vd();
    }

}