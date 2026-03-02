package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanForm;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class NewVanUC {
    private final VanRepository vans;
    private final ContentStore content;

    @Transactional
    public Van createVan(VanForm form) {
        var created = _createVan(form);

        var docnames = created.getImages();
        var docs = form.getImages();

        for (int i = 0; i < docnames.size(); i++) {
            content.save(docnames.get(i), docs.get(i));
        }

        return vans.save(created);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Van _createVan(VanForm form) {
        var model = new Model(form.getWmi(), form.getVi());

        var images = form.getImages().stream()
                .map(img -> "attachments/vans/" + UUID.randomUUID() + "-" + img.getOriginalFilename())
                .toList();

        var van = new Van();
        van.setModel(model);
        van.setRegistrationNumber(form.getRegistrationNumber());
        van.setMaxPayload(form.getMaxPayload());
        van.setImages(images);
        return vans.save(van);
    }

}
