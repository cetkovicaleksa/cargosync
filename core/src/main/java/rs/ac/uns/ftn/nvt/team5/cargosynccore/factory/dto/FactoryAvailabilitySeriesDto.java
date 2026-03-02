package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.util.List;
import java.util.UUID;

public record FactoryAvailabilitySeriesDto(
        UUID factoryId,
        String bucket,
        long totalOnlineSeconds,
        long totalOfflineSeconds,
        double onlinePercent,
        double offlinePercent,
        List<FactoryAvailabilityPointDto> points
) {}
