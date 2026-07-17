package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.GeoLocation;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanDistanceSeriesDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanStatusDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.VanStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.VanStatusRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Service
public class VanAnalyticsService {

    private final VanService vanService;
    private final VanStatusRepository statuses;
    private final InfluxService influx;

    @Transactional(readOnly = true)
    public VanStatusDto getStatus(String vin) {
        vanService.getVan(vin);
        VanStatus status = statuses.findById(vin).orElseGet(() -> new VanStatus(vin));
        return new VanStatusDto(vin, status.isOnline(), status.getLastHeartBeat(), toDto(status.getLastSeenAt()));
    }

    @Transactional(readOnly = true)
    public VanDistanceSeriesDto getDistanceSeries(String vin, Instant from, Instant to) {
        vanService.getVan(vin);
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BadRequestException("Invalid distance analytics range");
        }

        String bucket = resolveDistanceBucket(from, to);
        List<rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanDistancePointDto> points = influx.queryVanDistance(vin, from, to, bucket);
        double totalDistance = points.stream().mapToDouble(rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.VanDistancePointDto::distance).sum();
        return new VanDistanceSeriesDto(vin, bucket, totalDistance, points);
    }

    private String resolveDistanceBucket(Instant from, Instant to) {
        long days = Math.max(1, Duration.between(from, to).toDays());
        if (days <= 14) return "day";
        if (days <= 180) return "week";
        return "month";
    }

    private rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto toDto(GeoLocation location) {
        if (location == null) {
            return null;
        }

        var dto = new rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.dto.GeoLocationDto();
        dto.setLatitude(location.getLatitude());
        dto.setLongitude(location.getLongitude());
        return dto;
    }
}