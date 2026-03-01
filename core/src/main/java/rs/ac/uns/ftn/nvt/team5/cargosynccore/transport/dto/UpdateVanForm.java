package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Data
public class UpdateVanForm {

    @JsonIgnore
    private String vin;

    @Nullable
    @Min(5)
    @Max(20)
    private String registrationNumber;

    @Nullable
    @Positive
    private Integer maxPayload;

    @Nullable
    private List<MultipartFile> newImages;

    @Nullable
    private Set<String> removeImages;

}
