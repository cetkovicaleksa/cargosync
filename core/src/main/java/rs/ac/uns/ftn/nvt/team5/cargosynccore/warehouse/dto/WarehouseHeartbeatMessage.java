package rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.dto;

import java.time.Instant;
import java.util.UUID;

public record WarehouseHeartbeatMessage(UUID warehouseId, Instant timestamp) {}
