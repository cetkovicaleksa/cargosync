package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.ContentStore;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.UUID;
import java.util.regex.Pattern;

import static rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van.VanPK;

@RequiredArgsConstructor
@Service
class VanService {

    private final VanRepository vans;
    private final ContentStore content;

    VanPK parseVanVin(String vin) {
        var pattern = Pattern.compile("([^-]{3})-([^-]{6})-([^-]?.+)");
        var matcher = pattern.matcher(vin);

        if (!matcher.matches()) {
            throw new BadRequestException("Invalid vehicle identification number: " + vin);
        }

        String wmi = matcher.group(1),
                vd  = matcher.group(2),
                vi  = matcher.group(3);

        return new VanPK(wmi, vd, vi);
    }

    @Transactional(readOnly = true)
    public Van getVan(String vin) {
        var id = this.parseVanVin(vin);
        return getVan(id);
    }

    @Transactional(readOnly = true)
    public Van getVan(String wmi, Model model) {
        return getVan(new VanPK(wmi, model));
    }

    @Transactional(readOnly = true)
    public Van getVan(String wmi, String vd, String vi) {
        return getVan(new VanPK(wmi, vd, vi));
    }

    @Transactional(readOnly = true)
    public Van getVan(VanPK id) {
        return vans.findById(id)
                .orElseThrow(() -> new NotFoundException("Van not found"));
    }

    String saveVanImage(Van van, MultipartFile image) {
        String path = "images/vans/" + UUID.randomUUID() + "-" + image.getOriginalFilename();
        content.save(path, image);
        return path;
    }

}
