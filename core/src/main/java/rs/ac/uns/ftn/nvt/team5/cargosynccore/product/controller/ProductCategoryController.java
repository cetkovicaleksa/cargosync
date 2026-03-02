package rs.ac.uns.ftn.nvt.team5.cargosynccore.product.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.service.ProductService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class ProductCategoryController {
    private final ProductService productService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<String> getAllCategoryNames() {
        return productService.getAllCategoryNames();
    }
}
