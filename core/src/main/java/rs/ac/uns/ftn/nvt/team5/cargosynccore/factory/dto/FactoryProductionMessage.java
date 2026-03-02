package rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FactoryProductionMessage(
        @JsonProperty("factory_id") UUID factoryId,
        String type,
        Instant timestamp,
        List<ProductReading> readings
) {
    public record ProductReading(
            @JsonProperty("product_id") UUID productId,
            @JsonProperty("product_name") String productName,
            long amount
    ) {}
}
