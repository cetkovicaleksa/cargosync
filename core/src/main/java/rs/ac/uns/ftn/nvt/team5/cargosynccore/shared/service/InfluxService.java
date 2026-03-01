package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.config.properties.InfluxProperties;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class InfluxService {

    private final InfluxProperties props;
    private WriteApiBlocking writeApi;

    @PostConstruct
    void init() {
        if (props.token() == null || props.token().isBlank()) {
            log.warn("InfluxDB token not configured — metrics disabled");
            return;
        }
        InfluxDBClient client = InfluxDBClientFactory.create(
                props.url(), props.token().toCharArray(), props.org(), props.bucket());
        writeApi = client.getWriteApiBlocking();
    }

    public void writeWarehouseTemperature(UUID warehouseId, String sector, double temp, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("temperature")
                    .addTag("warehouse_id", warehouseId.toString())
                    .addTag("sector", sector)
                    .addField("value", temp)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write temperature to InfluxDB", e);
        }
    }

    public void writeWarehouseAvailabilityEvent(UUID warehouseId, boolean online, Instant time) {
        if (writeApi == null) return;
        try {
            var point = Point.measurement("warehouse_availability")
                    .addTag("warehouse_id", warehouseId.toString())
                    .addField("online", online)
                    .time(time, WritePrecision.MS);
            writeApi.writePoint(point);
        } catch (Exception e) {
            log.error("Failed to write availability event to InfluxDB", e);
        }
    }
}
