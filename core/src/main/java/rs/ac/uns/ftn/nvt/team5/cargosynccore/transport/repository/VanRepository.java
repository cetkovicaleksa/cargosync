package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Van;

import java.util.Optional;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface VanRepository extends JpaRepository<Van, Van.VanPK>, JpaSpecificationExecutor<Van> {
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Van v WHERE v.vi = :vi AND v.model.id.vd = :vd AND v.model.id.wmi = :wmi")
    Optional<Van> findByWmiAndVdAndVi_pwlock(String wmi, String vd, String vi);
}