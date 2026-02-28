package rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {
    Optional<Account> findByUsername(String username);
    boolean existsByUsername(String username);
    @Query("SELECT a " +
            "FROM Account a " +
            "JOIN a.owner o " +
            "WHERE o.email = ?1")
    Optional<Account> findByEmail(String email);
}