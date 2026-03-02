package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReasonDto {

    @NotNull
    @Size(min = 5)
    private String reason;

}
