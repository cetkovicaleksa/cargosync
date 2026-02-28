package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.dto;

import rs.ac.uns.ftn.nvt.team5.cargosynccore.factory.model.Factory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Product;

import java.util.stream.Collectors;

public class ProductMapper {
    private ProductMapper() {}

    public static ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getImageUrl(),
                product.getPrice(),
                product.getWeight(),
                product.isForSale(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getFactories().stream().map(Factory::getId).collect(Collectors.toSet()),
                product.getVersion()
        );
    }
}
