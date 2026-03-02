package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
