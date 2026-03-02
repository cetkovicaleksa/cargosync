package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CityOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.CountryOptionDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Country;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CityRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CountryService {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    @Cacheable(value = "countries", key = "'all'")
    public List<CountryOptionDto> findAllSorted() {
        Sort sort = Sort.by(Sort.Order.asc("name").ignoreCase());
        return countryRepository.findAll(sort)
                .stream()
                .map(c -> new CountryOptionDto(c.getCode(), c.getName()))
                .collect(Collectors.toList());
    }

    @Cacheable(value = "cities", key = "#countryCode")
    public List<CityOptionDto> findAllCitiesByCountrySorted(String countryCode) {
        return cityRepository.findByCountryCodeOrderByNameAsc(countryCode)
                .stream()
                .map(c -> new CityOptionDto(c.getCode(), c.getName()))
                .collect(Collectors.toList());
    }
}
