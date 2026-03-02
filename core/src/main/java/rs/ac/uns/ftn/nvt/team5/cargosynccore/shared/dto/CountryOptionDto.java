package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto;

import java.io.Serializable;

public record CountryOptionDto(
        String code,
        String name
) implements Serializable {}
