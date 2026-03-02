package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Customer;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Manager;

import java.util.Optional;
import java.util.UUID;

public interface ManagerRepository extends JpaRepository<Manager, UUID> {
    Optional<Manager> findByAvatar(String avatar);
}