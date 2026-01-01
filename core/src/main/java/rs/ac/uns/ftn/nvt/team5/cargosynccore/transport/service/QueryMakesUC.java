package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TransactionRequiredException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.InternalServerError;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.MakeRepository;

@RequiredArgsConstructor
@Transactional(readOnly=true)
@Service
public class QueryMakesUC {

    private final MakeRepository makes;
    private final EntityManager em;

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
        try {
            var existingMake = new Make();
            existingMake.setWmi(make.getWmi());

            existingMake = em.merge(existingMake);
            return new PageImpl<>(existingMake.getModels());
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Make not found");
        } catch (TransactionRequiredException e) {
            throw new InternalServerError("Transaction required", e);
        }
    }

}
