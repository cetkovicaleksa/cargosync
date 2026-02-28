package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import java.util.UUID;

public interface FactoryRepository extends JpaRepository<Factory, UUID>, JpaSpecificationExecutor<Factory> {
}