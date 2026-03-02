package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactoryRepository extends JpaRepository<Factory, UUID>, JpaSpecificationExecutor<Factory> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM Factory f WHERE f.id = :id")
    Optional<Factory> findByIdForUpdate(@Param("id") UUID id);
}