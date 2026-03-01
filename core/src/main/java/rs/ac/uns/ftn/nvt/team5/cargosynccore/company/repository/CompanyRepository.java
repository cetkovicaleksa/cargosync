package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.Company;

import java.util.List;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
    List<Company> findAllByOwner_id(UUID id);
}
