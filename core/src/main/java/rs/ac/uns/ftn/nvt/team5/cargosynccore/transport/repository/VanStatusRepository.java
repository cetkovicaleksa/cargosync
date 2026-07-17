package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.VanStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface VanStatusRepository extends JpaRepository<VanStatus, String> {

	Optional<VanStatus> findByVin(String vin);

	List<VanStatus> findByOnlineTrueAndLastHeartBeatBefore(Instant threshold);
}