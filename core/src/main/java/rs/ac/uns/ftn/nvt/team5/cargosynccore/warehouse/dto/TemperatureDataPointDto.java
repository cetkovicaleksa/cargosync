package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import java.time.Instant;

public record TemperatureDataPointDto(Instant time, Double temperature) {}
