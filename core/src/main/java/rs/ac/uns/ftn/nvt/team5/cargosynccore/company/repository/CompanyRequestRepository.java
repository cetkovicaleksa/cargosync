package rs.ac.uns.ftn.nvt.team5.cargosynccore.company.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.CompanyRequest;

import java.util.List;
import java.util.UUID;

interface CompanyRequestRepository extends JpaRepository<CompanyRequest, UUID> {
    List<CompanyRequest> findAllByStatus(CompanyRequest.Status status);
    List<CompanyRequest> findAllByStatus(CompanyRequest.Status status, Sort sort);
    Page<CompanyRequest> findAllByStatus(CompanyRequest.Status status, Pageable pageable);
}
