package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.UpdateVanForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.MakeRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class UpdateVanUC {
    private final VanService vanService;
    private final VanRepository vans;
        private final MakeRepository makes;

    @Transactional
    public Van updateVan(UpdateVanForm vanRequest) {
        var van = vanService.getVan(vanRequest.getVin());
        van.setVersion(vanRequest.getVersion());

                Optional.ofNullable(vanRequest.getModel())
                                .ifPresent(model -> van.setModel(resolveModel(model.getWmi(), model.getVd())));

        Optional.ofNullable(vanRequest.getRegistrationNumber())
                .ifPresent(van::setRegistrationNumber);
        Optional.ofNullable(vanRequest.getMaxPayload())
                .ifPresent(van::setMaxPayload);
        Optional.ofNullable(vanRequest.getRemoveImages())
                .ifPresent(van.getImages()::removeAll); //

        return vans.save(van);
    }

        private Model resolveModel(String wmi, String vd) {
                Make make = makes.findById(wmi)
                                .orElseThrow(() -> new IllegalArgumentException("Unknown van make: " + wmi));

                return make.getModels().stream()
                                .filter(model -> vd.equals(model.getVd()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Unknown van model: " + wmi + " " + vd));
        }

}
