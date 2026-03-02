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

    private TemperatureDataPointDto toDataPoint(FluxRecord record) {
        Instant time = record.getTime();
        Object val   = record.getValue();
        Double temp  = val instanceof Number n ? n.doubleValue() : null;
        return new TemperatureDataPointDto(time, temp);
    }
}
