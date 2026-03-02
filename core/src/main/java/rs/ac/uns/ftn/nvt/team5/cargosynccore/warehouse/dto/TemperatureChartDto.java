package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public record TemperatureChartDto(
        UUID sectorId,
        String sectorName,
        Double lastTemperature,
        Instant lastTemperatureAt,
        String windowPeriod,
        Instant rangeStart,
        Instant rangeStop,
        List<TemperatureDataPointDto> dataPoints
) {}
