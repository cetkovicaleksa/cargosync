package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.TemperatureChartDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.TemperatureChartPeriod;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.TemperatureDataPointDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseSector;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseSectorRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TemperatureChartService {

    private final WarehouseSectorRepository sectorRepository;
    private final InfluxService influxService;

    @Transactional(readOnly = true)
    public TemperatureChartDto getChart(
            UUID warehouseId,
            UUID sectorId,
            TemperatureChartPeriod period,
            Instant from,
            Instant to
    ) {
        WarehouseSector sector = sectorRepository.findByIdAndWarehouseId(sectorId, warehouseId)
                .orElseThrow(() -> new NotFoundException("Sector not found for this warehouse"));

        Instant now = Instant.now();
        Instant rangeStop;
        Instant rangeStart;
        String  windowPeriod;

        if (period == TemperatureChartPeriod.CUSTOM) {
            if (from == null || to == null) {
                throw new BadRequestException("'from' and 'to' are required for CUSTOM period");
            }
            if (!from.isBefore(to)) {
                throw new BadRequestException("'from' must be before 'to'");
            }
            long days = ChronoUnit.DAYS.between(from, to);
            if (days > 366) {
                throw new BadRequestException("Custom range must not exceed 1 year");
            }
            rangeStart   = from;
            rangeStop    = to;
            windowPeriod = resolveWindowForDays(days);
        } else {
            rangeStop    = now;
            rangeStart   = switch (period) {
                case LAST_7D  -> now.minus(7,   ChronoUnit.DAYS);
                case LAST_30D -> now.minus(30,  ChronoUnit.DAYS);
                case LAST_3M  -> now.minus(90,  ChronoUnit.DAYS);
                case LAST_6M  -> now.minus(180, ChronoUnit.DAYS);
                case LAST_1Y  -> now.minus(365, ChronoUnit.DAYS);
                default       -> throw new BadRequestException("Unsupported period: " + period);
            };
            windowPeriod = switch (period) {
                case LAST_7D, LAST_30D -> "1d";
                case LAST_3M, LAST_6M  -> "1w";
                case LAST_1Y           -> "1mo";
                default                -> "1d";
            };
        }

        List<TemperatureDataPointDto> dataPoints = influxService.queryTemperature(
                warehouseId.toString(),
                sector.getName(),
                rangeStart,
                rangeStop,
                windowPeriod
        );

        return new TemperatureChartDto(
                sector.getId(),
                sector.getName(),
                sector.getLastTemperature(),
                sector.getLastTemperatureAt(),
                windowPeriod,
                rangeStart,
                rangeStop,
                dataPoints
        );
    }

    /** Same granularity logic as preset periods, applied to custom ranges. */
    private String resolveWindowForDays(long days) {
        if (days <= 30)  return "1d";
        if (days <= 180) return "1w";
        return "1mo";
    }
}
