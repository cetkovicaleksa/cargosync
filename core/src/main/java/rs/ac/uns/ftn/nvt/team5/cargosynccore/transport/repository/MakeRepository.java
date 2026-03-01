package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make;

public interface MakeRepository extends JpaRepository<Make, String> {
}