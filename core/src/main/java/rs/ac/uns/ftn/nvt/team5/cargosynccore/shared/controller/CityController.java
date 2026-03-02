package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CityOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CountryOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.CountryService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/cities")
public class CityController {
    private final CountryService countryService;

    @GetMapping()
    @PreAuthorize("isAuthenticated()")
    public List<CityOptionDto> getCitiesByCountry(@RequestParam String countryCode) {
        return countryService.findAllCitiesByCountrySorted(countryCode);
    }
}
