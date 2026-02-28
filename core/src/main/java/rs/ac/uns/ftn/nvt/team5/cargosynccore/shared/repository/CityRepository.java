package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;

import java.lang.ScopedValue;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, City.CityPK> {
    Optional<City> findByCountryCodeAndCode(@NotBlank String countryCode, @NotBlank String code);
}
