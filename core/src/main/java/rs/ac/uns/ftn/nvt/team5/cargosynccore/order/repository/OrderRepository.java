package rs.ac.uns.ftn.nvt.team5.cargosynccore.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.order.model.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCustomerIdOrderByCreatedDateDesc(UUID customerId, Pageable pageable);

    Optional<Order> findByIdAndCustomerId(UUID id, UUID customerId);
}
