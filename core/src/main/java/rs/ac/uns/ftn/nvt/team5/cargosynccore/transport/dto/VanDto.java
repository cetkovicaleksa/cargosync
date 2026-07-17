package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * DTO for {@link rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van}
 */
@Data
public class VanDto implements Serializable {

    private String vin, vi, registrationNumber;
    private ModelDto model;
    private int maxPayload;
    private List<String> images;
    private int version;

//    @JsonProperty("id")
//    public String getId4json() {
//        return vin;
//    }

}