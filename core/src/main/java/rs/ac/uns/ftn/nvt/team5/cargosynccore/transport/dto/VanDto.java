package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van}
 */
@Data
public class VanDto implements Serializable {

    private String vin, registrationNumber;
    private ModelDto model;
    private int maxPayload;

//    @JsonProperty("id")
//    public String getId4json() {
//        return vin;
//    }

}