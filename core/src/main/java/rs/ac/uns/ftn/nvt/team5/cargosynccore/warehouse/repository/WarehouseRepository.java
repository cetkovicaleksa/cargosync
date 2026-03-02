package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;

import java.util.Optional;
import java.util.UUID;

public interface WarehouseRepository extends JpaRepository<Warehouse, UUID>, JpaSpecificationExecutor<Warehouse> {

    @Query("SELECT w FROM Warehouse w LEFT JOIN FETCH w.sectors LEFT JOIN FETCH w.city c LEFT JOIN FETCH c.country WHERE w.id = :id")
    Optional<Warehouse> findByIdWithSectors(@Param("id") UUID id);

    boolean existsByName(String name);

    Optional<Warehouse> findByName(String name);
}
