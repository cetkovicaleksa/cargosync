package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;

public interface VanRepository extends JpaRepository<Van, Van.VanPK>, JpaSpecificationExecutor<Van> {
}