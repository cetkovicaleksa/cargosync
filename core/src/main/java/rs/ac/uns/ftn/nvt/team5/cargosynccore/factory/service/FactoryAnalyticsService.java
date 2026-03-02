package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.service;

import lombok.RequiredArgsConstructor;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryAvailabilityPointDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryAvailabilitySeriesDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryProductionPointDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto.FactoryProductionSeriesDto;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.repository.FactoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.BadRequestException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service.InfluxService;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactoryAnalyticsService {

    private final FactoryRepository factoryRepository;
    private final InfluxService influxService;

    @Transactional(readOnly = true)
    public FactoryProductionSeriesDto getProductionSeries(UUID factoryId, UUID productId, Instant from, Instant to) {
        validateRange(from, to);
        requireFactory(factoryId);

        String bucket = resolveProductionBucket(from, to);
        String fluxWindow = toFluxWindow(bucket);

        List<FactoryProductionPointDto> points = influxService.queryFactoryProduction(
                        factoryId.toString(),
                        productId.toString(),
                        from,
                        to,
                        fluxWindow)
                .stream()
                .map(aggregate -> {
                    Instant pointStart = aggregate.periodStart();
                    Instant pointEnd = minInstant(nextBucket(pointStart, bucket), to);
                    return new FactoryProductionPointDto(pointStart, pointEnd, Math.max(0L, aggregate.units()));
                })
                .toList();

        long totalUnits = points.stream().mapToLong(FactoryProductionPointDto::units).sum();
        return new FactoryProductionSeriesDto(factoryId, productId, bucket, totalUnits, points);
    }

    @Transactional(readOnly = true)
    public FactoryAvailabilitySeriesDto getAvailabilitySeries(UUID factoryId, Instant from, Instant to) {
        validateRange(from, to);
        requireFactory(factoryId);

        String bucket = resolveAvailabilityBucket(from, to);
        List<TimeBucket> buckets = buildBuckets(from, to, bucket);

        var availabilityEvents = influxService.queryFactoryAvailabilityEvents(factoryId.toString(), from, to);
        String previousStatus = influxService.queryFactoryAvailabilityLastStatusBefore(factoryId.toString(), from).orElse(null);
        if ((previousStatus == null || previousStatus.isBlank()) && availabilityEvents.isEmpty()) {
            List<FactoryAvailabilityPointDto> emptyPoints = buckets.stream()
                .map(bucketValue -> new FactoryAvailabilityPointDto(
                    bucketValue.start,
                    bucketValue.end,
                    0L,
                    0L,
                    0.0,
                    0.0
                ))
                .toList();

            return new FactoryAvailabilitySeriesDto(
                factoryId,
                bucket,
                0L,
                0L,
                0.0,
                0.0,
                emptyPoints
            );
        }

        boolean currentOnline = "online".equalsIgnoreCase(previousStatus);
        Instant cursor = from;

        for (var event : availabilityEvents) {
            if (event.timestamp() == null) {
                continue;
            }
            Instant eventTime = clamp(event.timestamp(), from, to);
            if (eventTime.isAfter(cursor)) {
                allocateDuration(buckets, cursor, eventTime, currentOnline);
            }
            currentOnline = "online".equalsIgnoreCase(event.status());
            cursor = eventTime;
        }

        if (to.isAfter(cursor)) {
            allocateDuration(buckets, cursor, to, currentOnline);
        }

        List<FactoryAvailabilityPointDto> points = buckets.stream()
                .map(bucketValue -> {
                    long totalSeconds = Math.max(1L, bucketValue.onlineSeconds + bucketValue.offlineSeconds);
                    double onlinePercent = round2((bucketValue.onlineSeconds * 100.0) / totalSeconds);
                    double offlinePercent = round2((bucketValue.offlineSeconds * 100.0) / totalSeconds);
                    return new FactoryAvailabilityPointDto(
                            bucketValue.start,
                            bucketValue.end,
                            bucketValue.onlineSeconds,
                            bucketValue.offlineSeconds,
                            onlinePercent,
                            offlinePercent
                    );
                })
                .toList();

        long totalOnlineSeconds = points.stream().mapToLong(FactoryAvailabilityPointDto::onlineSeconds).sum();
        long totalOfflineSeconds = points.stream().mapToLong(FactoryAvailabilityPointDto::offlineSeconds).sum();
        long totalSeconds = Math.max(1L, totalOnlineSeconds + totalOfflineSeconds);

        return new FactoryAvailabilitySeriesDto(
                factoryId,
                bucket,
                totalOnlineSeconds,
                totalOfflineSeconds,
                round2((totalOnlineSeconds * 100.0) / totalSeconds),
                round2((totalOfflineSeconds * 100.0) / totalSeconds),
                points
        );
    }

    private void requireFactory(UUID factoryId) {
        if (!factoryRepository.existsById(factoryId)) {
            throw new NotFoundException("Factory not found");
        }
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new BadRequestException("'from' and 'to' are required");
        }
        if (!from.isBefore(to)) {
            throw new BadRequestException("'from' must be before 'to'");
        }

        long days = ChronoUnit.DAYS.between(from, to);
        if (days > 366) {
            throw new BadRequestException("Range must not exceed 1 year");
        }
    }

    private String resolveProductionBucket(Instant from, Instant to) {
        long days = Math.max(1, ChronoUnit.DAYS.between(from, to));
        if (days <= 45) {
            return "day";
        }
        if (days <= 180) {
            return "week";
        }
        return "month";
    }

    private String resolveAvailabilityBucket(Instant from, Instant to) {
        long hours = Math.max(1, ChronoUnit.HOURS.between(from, to));
        long days = Math.max(1, ChronoUnit.DAYS.between(from, to));

        if (hours <= 48) {
            return "hour";
        }
        if (days <= 45) {
            return "day";
        }
        if (days <= 180) {
            return "week";
        }
        return "month";
    }

    private String toFluxWindow(String bucket) {
        return switch (bucket) {
            case "day" -> "1d";
            case "week" -> "1w";
            case "month" -> "1mo";
            default -> throw new BadRequestException("Unsupported bucket: " + bucket);
        };
    }

    private List<TimeBucket> buildBuckets(Instant from, Instant to, String bucket) {
        List<TimeBucket> buckets = new ArrayList<>();

        Instant cursor = startOfBucket(from, bucket);
        while (cursor.isBefore(to)) {
            Instant next = nextBucket(cursor, bucket);
            Instant effectiveStart = maxInstant(cursor, from);
            Instant effectiveEnd = minInstant(next, to);

            if (effectiveEnd.isAfter(effectiveStart)) {
                buckets.add(new TimeBucket(effectiveStart, effectiveEnd));
            }
            cursor = next;
        }

        if (buckets.isEmpty()) {
            buckets.add(new TimeBucket(from, to));
        }
        return buckets;
    }

    private Instant startOfBucket(Instant value, String bucket) {
        LocalDateTime dt = LocalDateTime.ofInstant(value, ZoneOffset.UTC);

        return switch (bucket) {
            case "hour" -> dt.withMinute(0).withSecond(0).withNano(0).toInstant(ZoneOffset.UTC);
            case "day" -> dt.toLocalDate().atStartOfDay().toInstant(ZoneOffset.UTC);
            case "week" -> dt.toLocalDate()
                    .with(DayOfWeek.MONDAY)
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            case "month" -> dt.withDayOfMonth(1)
                    .toLocalDate()
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC);
            default -> value;
        };
    }

    private Instant nextBucket(Instant value, String bucket) {
        return switch (bucket) {
            case "hour" -> value.plus(1, ChronoUnit.HOURS);
            case "day" -> value.plus(1, ChronoUnit.DAYS);
            case "week" -> value.plus(7, ChronoUnit.DAYS);
            case "month" -> LocalDateTime.ofInstant(value, ZoneOffset.UTC)
                    .plusMonths(1)
                    .toInstant(ZoneOffset.UTC);
            default -> value.plus(1, ChronoUnit.DAYS);
        };
    }

    private void allocateDuration(List<TimeBucket> buckets, Instant segmentStart, Instant segmentEnd, boolean online) {
        if (!segmentEnd.isAfter(segmentStart)) {
            return;
        }

        for (TimeBucket bucket : buckets) {
            Instant overlapStart = maxInstant(bucket.start, segmentStart);
            Instant overlapEnd = minInstant(bucket.end, segmentEnd);
            if (!overlapEnd.isAfter(overlapStart)) {
                continue;
            }

            long seconds = Duration.between(overlapStart, overlapEnd).getSeconds();
            if (seconds <= 0) {
                continue;
            }

            if (online) {
                bucket.onlineSeconds += seconds;
            } else {
                bucket.offlineSeconds += seconds;
            }
        }
    }

    private Instant clamp(Instant value, Instant min, Instant max) {
        if (value.isBefore(min)) return min;
        if (value.isAfter(max)) return max;
        return value;
    }

    private Instant maxInstant(Instant left, Instant right) {
        return left.isAfter(right) ? left : right;
    }

    private Instant minInstant(Instant left, Instant right) {
        return left.isBefore(right) ? left : right;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static final class TimeBucket {
        private final Instant start;
        private final Instant end;
        private long onlineSeconds;
        private long offlineSeconds;

        private TimeBucket(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }
    }
}
