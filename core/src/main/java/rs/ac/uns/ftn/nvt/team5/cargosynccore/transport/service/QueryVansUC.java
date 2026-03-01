package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanQueryParams;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanRepository;

import java.util.Objects;

import static rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanSpecifications.*;


@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class QueryVansUC {

    private final VanRepository vans;
    private final VanService vanService;

    public Page<Van> getAllVans() {
        return vans.findAll(Pageable.unpaged());
    }

    public Page<Van> getAllVans(@NotNull VanQueryParams params) {
        Objects.requireNonNull(params, "VanQueryParams must not be null");

        var spec = Specification
                .where(matchesQuery(params.getQ()))
                .and(makeIn(params.getMake()))
                .and(modelIn(params.getModel()))
                .and(maxPayloadGte(params.getCanCarryKg()))
                .and(maxPayloadLte(params.getTooBigIfCanCarryKg()));

        return vans.findAll(spec, params.getPageable());
    }

    public Van getVan(String vin) {
        return vanService.getVan(vin);
    }

}
