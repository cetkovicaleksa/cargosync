package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.regex.Pattern;

import static rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van.VanPK;

@RequiredArgsConstructor
@Service
class VanService {

    private final VanRepository vans;

    static VanPK parseVanVin(String vin) {
        Pattern pattern = Pattern.compile("([^-]+)-([^-]+)-([^-]+)"); // maybe add stricter pattern
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
        var id = VanService.parseVanVin(vin);
        return getVan(id);
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

}
