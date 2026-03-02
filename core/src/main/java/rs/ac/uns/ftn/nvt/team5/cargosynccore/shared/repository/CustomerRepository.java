package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Customer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByAvatar(String avatar);
}