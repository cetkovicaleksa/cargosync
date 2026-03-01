package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Data
public class VanForm {

    @NotNull
    private ModelForm model;

    @NotNull
    @Min(5)
    @Max(20)
    private String registrationNumber;

    @NotNull
    @Positive
    private Integer maxPayload;

    @NotNull
    @Size(min = 1, max = 32)
    private List<MultipartFile> images;

    @Data
    public static class ModelForm {
        private String wmi;
        private String vd;
    }

    public String getWmi() {
        return Optional.ofNullable(model)
                .map(ModelForm::getWmi)
                .orElse(null);
    }

    public String getVi() {
        return Optional.ofNullable(model)
                .map(ModelForm::getVd)
                .orElse(null);
    }

}
