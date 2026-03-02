package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.time.LocalDateTime;

public record FactoryStatusDto(
    boolean isOnline,
    LocalDateTime lastHeartbeat
) {}
