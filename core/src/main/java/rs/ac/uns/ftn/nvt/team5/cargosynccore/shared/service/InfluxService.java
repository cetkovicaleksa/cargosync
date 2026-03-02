package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.InfluxProperties;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto.TemperatureDataPointDto;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfluxService {

    private final InfluxProperties props;
    private InfluxDBClient client;
    private WriteApiBlocking writeApi;

    @PostConstruct
    void init() {
        if (props.token() == null || props.token().isBlank()) {
            log.warn("InfluxDB token not configured — metrics disabled");
            return;
        }
        log.info("InfluxDB configured url={} org={} bucket={}", props.url(), props.org(), props.bucket());
        client = InfluxDBClientFactory.create(
                props.url(), props.token().toCharArray(), props.org(), props.bucket());
        writeApi = client.getWriteApiBlocking();
    }

    public void writeWarehouseTemperature(UUID warehouseId, String sectorId, String sectorName, double temp, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("warehouse_temperature")
                    .addTag("warehouse_id", warehouseId.toString())
                    .addTag("sector_id", sectorId != null ? sectorId : "")
                    .addTag("sector_name", sectorName)
                    .addField("temperature", temp)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write temperature to InfluxDB", e);
        }
    }

    public void writeWarehouseAvailabilityEvent(UUID warehouseId, String status, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("warehouse_availability")
                    .addTag("warehouse_id", warehouseId.toString())
                    .addField("status", status)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write availability event to InfluxDB", e);
        }
    }

    public void writeFactoryAvailabilityEvent(UUID factoryId, String status, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("factory_availability")
                    .addTag("factory_id", factoryId.toString())
                    .addField("status", status)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write factory availability event to InfluxDB", e);
        }
    }

    public void writeFactoryProduction(UUID factoryId, UUID productId, long amount, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("production")
                    .addTag("factory_id", factoryId.toString())
                    .addTag("product_id", productId.toString())
                    .addField("amount", amount)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write factory production event to InfluxDB", e);
        }
    }

    /**
     * Query averaged temperature readings for a specific warehouse sector.
     *
     * @param warehouseId  warehouse UUID string
     * @param sectorName   sector name (as stored in InfluxDB tag)
     * @param start        range start
     * @param stop         range stop
     * @param windowPeriod Flux duration string: "1d", "1w", "1mo"
     * @return list of averaged data points
     */
    public List<TemperatureDataPointDto> queryTemperature(
            String warehouseId,
            String sectorName,
            Instant start,
            Instant stop,
            String windowPeriod
    ) {
        if (client == null) return List.of();

        String flux = buildTemperatureFlux(warehouseId, sectorName, start, stop, windowPeriod);
        try {
            QueryApi queryApi = client.getQueryApi();
            return queryApi.query(flux).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(this::toDataPoint)
                    .filter(dp -> dp.temperature() != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("InfluxDB temperature query failed for warehouse={} sector={}", warehouseId, sectorName, e);
            return List.of();
        }
    }

    public List<FactoryProductionAggregate> queryFactoryProduction(
            String factoryId,
            String productId,
            Instant start,
            Instant stop,
            String windowPeriod
    ) {
        if (client == null) return List.of();

        String flux = buildFactoryProductionFlux(factoryId, productId, start, stop, windowPeriod);
        try {
            QueryApi queryApi = client.getQueryApi();
            return queryApi.query(flux).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(record -> new FactoryProductionAggregate(
                            record.getTime(),
                            record.getValue() instanceof Number n ? n.longValue() : 0L))
                    .toList();
        } catch (Exception e) {
            log.error("InfluxDB production query failed for factory={} product={}", factoryId, productId, e);
            return List.of();
        }
    }

    public List<FactoryAvailabilityEvent> queryFactoryAvailabilityEvents(
            String factoryId,
            Instant start,
            Instant stop
    ) {
        if (client == null) return List.of();

        String flux = buildFactoryAvailabilityFlux(factoryId, start, stop);
        try {
            QueryApi queryApi = client.getQueryApi();
            return queryApi.query(flux).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(this::toFactoryAvailabilityEvent)
                    .filter(event -> event.timestamp() != null && event.status() != null)
                    .toList();
        } catch (Exception e) {
            log.error("InfluxDB availability query failed for factory={}", factoryId, e);
            return List.of();
        }
    }

    public Optional<String> queryFactoryAvailabilityLastStatusBefore(String factoryId, Instant before) {
        if (client == null) return Optional.empty();

        String flux = buildFactoryLastAvailabilityStatusFlux(factoryId, before);
        try {
            QueryApi queryApi = client.getQueryApi();
            return queryApi.query(flux).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(record -> record.getValue() != null ? String.valueOf(record.getValue()) : null)
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst();
        } catch (Exception e) {
            log.error("InfluxDB availability previous-status query failed for factory={}", factoryId, e);
            return Optional.empty();
        }
    }

    private String buildTemperatureFlux(
            String warehouseId,
            String sectorName,
            Instant start,
            Instant stop,
            String windowPeriod
    ) {
        String startStr = DateTimeFormatter.ISO_INSTANT.format(start);
        String stopStr  = DateTimeFormatter.ISO_INSTANT.format(stop);
        // Escape single quotes in names coming from the database
        String safeWarehouseId = warehouseId.replace("'", "\\'");
        String safeSector      = sectorName.replace("'", "\\'");
        String safeBucket      = props.bucket().replace("\"", "\\\"");

        return String.format("""
                from(bucket: "%s")
                  |> range(start: %s, stop: %s)
                  |> filter(fn: (r) => r["_measurement"] == "warehouse_temperature")
                  |> filter(fn: (r) => r["warehouse_id"] == "%s")
                  |> filter(fn: (r) => r["sector_name"] == "%s")
                  |> filter(fn: (r) => r["_field"] == "temperature")
                  |> aggregateWindow(every: %s, fn: mean, createEmpty: false)
                  |> yield(name: "mean")
                """,
                safeBucket, startStr, stopStr, safeWarehouseId, safeSector, windowPeriod);
    }

        private String buildFactoryProductionFlux(
                        String factoryId,
                        String productId,
                        Instant start,
                        Instant stop,
                        String windowPeriod
        ) {
                String startStr = DateTimeFormatter.ISO_INSTANT.format(start);
                String stopStr = DateTimeFormatter.ISO_INSTANT.format(stop);
                String safeFactoryId = factoryId.replace("'", "\\'");
                String safeProductId = productId.replace("'", "\\'");
                String safeBucket = props.bucket().replace("\"", "\\\"");

                return String.format("""
                                from(bucket: "%s")
                                    |> range(start: %s, stop: %s)
                                    |> filter(fn: (r) => r["_measurement"] == "production")
                                    |> filter(fn: (r) => r["factory_id"] == "%s")
                                    |> filter(fn: (r) => r["product_id"] == "%s")
                                    |> filter(fn: (r) => r["_field"] == "amount")
                                    |> aggregateWindow(every: %s, fn: sum, createEmpty: false)
                                    |> sort(columns: ["_time"])
                                """, safeBucket, startStr, stopStr, safeFactoryId, safeProductId, windowPeriod);
        }

        private String buildFactoryAvailabilityFlux(String factoryId, Instant start, Instant stop) {
                String startStr = DateTimeFormatter.ISO_INSTANT.format(start);
                String stopStr = DateTimeFormatter.ISO_INSTANT.format(stop);
                String safeFactoryId = factoryId.replace("'", "\\'");
                String safeBucket = props.bucket().replace("\"", "\\\"");

                return String.format("""
                                from(bucket: "%s")
                                    |> range(start: %s, stop: %s)
                                    |> filter(fn: (r) => r["_measurement"] == "factory_availability")
                                    |> filter(fn: (r) => r["factory_id"] == "%s")
                                    |> filter(fn: (r) => r["_field"] == "status")
                                    |> sort(columns: ["_time"])
                                """, safeBucket, startStr, stopStr, safeFactoryId);
        }

        private String buildFactoryLastAvailabilityStatusFlux(String factoryId, Instant before) {
                String stopStr = DateTimeFormatter.ISO_INSTANT.format(before);
                String safeFactoryId = factoryId.replace("'", "\\'");
                String safeBucket = props.bucket().replace("\"", "\\\"");

                return String.format("""
                                from(bucket: "%s")
                                    |> range(start: time(v: 0), stop: %s)
                                    |> filter(fn: (r) => r["_measurement"] == "factory_availability")
                                    |> filter(fn: (r) => r["factory_id"] == "%s")
                                    |> filter(fn: (r) => r["_field"] == "status")
                                    |> sort(columns: ["_time"])
                                    |> tail(n: 1)
                                """, safeBucket, stopStr, safeFactoryId);
        }

    private TemperatureDataPointDto toDataPoint(FluxRecord record) {
        Instant time = record.getTime();
        Object val   = record.getValue();
        Double temp  = val instanceof Number n ? n.doubleValue() : null;
        return new TemperatureDataPointDto(time, temp);
    }

    private FactoryAvailabilityEvent toFactoryAvailabilityEvent(FluxRecord record) {
        Instant time = record.getTime();
        Object val = record.getValue();
        String status = val != null ? String.valueOf(val) : null;
        return new FactoryAvailabilityEvent(time, status);
    }

    public record FactoryProductionAggregate(Instant periodStart, long units) {}

    public record FactoryAvailabilityEvent(Instant timestamp, String status) {}
}
