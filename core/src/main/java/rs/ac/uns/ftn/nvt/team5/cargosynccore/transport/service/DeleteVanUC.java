package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

@RequiredArgsConstructor
@Service
public class DeleteVanUC {

    private final VanService vanService;
    private final VanRepository vans;

    @Transactional
    public void deleteVan(String vin) {
        var id = vanService.parseVanVin(vin);
        String
                vi = id.vi(),
                vd = id.model().getId().vd(),
                wmi = id.model().getId().wmi();

        var van = vans.findByWmiAndVdAndVi_pwlock(wmi, vd, vi)
                        .orElseThrow(() -> new NotFoundException("Van Not Found"));

        vans.delete(van);
    }

}
