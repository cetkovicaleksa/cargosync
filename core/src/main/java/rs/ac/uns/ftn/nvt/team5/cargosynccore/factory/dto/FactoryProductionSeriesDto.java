package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import java.util.List;
import java.util.UUID;

public record FactoryProductionSeriesDto(
        UUID factoryId,
        UUID productId,
        String bucket,
        long totalUnits,
        List<FactoryProductionPointDto> points
) {}
