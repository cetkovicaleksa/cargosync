package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.FactoryStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FactoryStatusRepository extends JpaRepository<FactoryStatus, UUID> {
    Optional<FactoryStatus> findByFactoryId(UUID factoryId);

    @Query("SELECT f FROM FactoryStatus f WHERE f.isOnline = true AND f.lastHeartbeat < :threshold")
    List<FactoryStatus> findStaleOnlineFactories(@Param("threshold") LocalDateTime threshold);

    void deleteFactoryStatusByFactoryId(UUID factoryId);
}