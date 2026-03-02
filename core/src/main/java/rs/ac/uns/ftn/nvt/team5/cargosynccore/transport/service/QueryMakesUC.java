package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.MakeRepository;

import static java.util.Objects.requireNonNull;

@RequiredArgsConstructor
@Transactional(readOnly=true)
@Service
public class QueryMakesUC {

    private final MakeRepository makes;

    public Page<Make> getAllMakes() {
        return makes.findAll(Pageable.unpaged());
    }

    public Make getMake(String wmi) {
        return makes.findById(wmi)
                .orElseThrow(() -> new NotFoundException("Make not found"));
    }

    public Page<Model> getAllMakeModels(String wmi) {
        return getAllMakeModels(new Make(wmi, null, null));
    }

    public Page<Model> getAllMakeModels(Make make) {
        return makes.findById(requireNonNull(make, "Make must not be null").getWmi())
                .map(Make::getModels)
                .map(PageImpl::new)
                .orElseThrow(() -> new NotFoundException("Make not found"));
    }

}
