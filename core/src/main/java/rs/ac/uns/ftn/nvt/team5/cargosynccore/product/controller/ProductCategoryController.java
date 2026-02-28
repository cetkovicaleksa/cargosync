package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.ProductCategory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.ProductCategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductCategoryRepository categoryRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<String> getAllCategoryNames() {
        return categoryRepository.findAll().stream()
                .map(ProductCategory::getName)
                .collect(Collectors.toList());
    }
}
