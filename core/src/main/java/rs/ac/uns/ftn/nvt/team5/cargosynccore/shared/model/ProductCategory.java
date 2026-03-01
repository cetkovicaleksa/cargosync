package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_categories")
public class ProductCategory extends AbstractPersistable {

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    public ProductCategory(String name) {
        this.name = name;
    }
}