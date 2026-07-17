package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto;

import java.util.List;

public record VanDistanceSeriesDto(
        String vanVin,
        String bucket,
        double totalDistance,
        List<VanDistancePointDto> points
) {}