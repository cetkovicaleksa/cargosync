package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryStatus;

import java.util.UUID;

public interface FactoryStatusRepository extends JpaRepository<FactoryStatus, UUID> {
    void deleteFactoryStatusByFactoryId(UUID factoryId);
}