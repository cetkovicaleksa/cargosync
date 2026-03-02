package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Data
public class UpdateVanForm {

    @Null
    private transient String vin; // not sure how to ignore this with form data

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

    @Null
    private transient Integer version;

}
