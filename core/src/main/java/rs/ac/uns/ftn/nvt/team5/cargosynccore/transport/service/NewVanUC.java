package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.List;

@RequiredArgsConstructor
@Service
public class NewVanUC {
    private final VanRepository vans;
    private final VanService vanService;

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public Van createVan(VanForm vanRequest) {
        var model = new Model(vanRequest.getWmi(), vanRequest.getVi());

        var van = new Van();
        van.setModel(model);
        van.setRegistrationNumber(vanRequest.getRegistrationNumber());
        van.setMaxPayload(vanRequest.getMaxPayload());
        van.setImages(List.of());

        var created = vans.saveAndFlush(van);
        var images = vanRequest.getImages().stream()
                .map(img -> vanService.saveVanImage(created, img))
                .toList();

        created.setImages(images);
        return vans.save(created);
    }

}
