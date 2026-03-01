package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.UpdateVanForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UpdateVanUC {
    private final VanService vanService;
    private final VanRepository vans;

    @Transactional
    public Van updateVan(UpdateVanForm vanRequest) {
        var van = vanService.getVan(vanRequest.getVin());
        van.setVersion(vanRequest.getVersion());

        Optional.ofNullable(vanRequest.getRegistrationNumber())
                .ifPresent(van::setRegistrationNumber);
        Optional.ofNullable(vanRequest.getMaxPayload())
                .ifPresent(van::setMaxPayload);
        Optional.ofNullable(vanRequest.getRemoveImages())
                .ifPresent(van.getImages()::removeAll); //

        return vans.save(van);
    }

}
