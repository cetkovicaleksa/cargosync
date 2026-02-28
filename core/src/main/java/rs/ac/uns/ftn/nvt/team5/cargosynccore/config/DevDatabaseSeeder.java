package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.*;

import java.math.BigDecimal;
import java.util.List;

@Profile("dev")
@RequiredArgsConstructor
@Component
public class DevDatabaseSeeder implements CommandLineRunner {

    private static final List<Country> builtinCountries = List.of(
      new Country("RS", "Serbia",
              List.of(
                      new City("NVS", "Novi Sad"),
                      new City("SUB", "Subotica"),
                      new City("BEG", "Beograd"),
                      new City("SRM", "Sremska Mitrovica"),
                      new City("PRN", "Priština")
              )
      ),
      new Country("BA", "Bosnia and Herzegovina",
              List.of(
                      new City("BJA", "Bijeljina"),
                      new City("BNX", "Banja Luka"),
                      new City("SJJ", "Sarajevo"),
                      new City("ZNK", "Zvornik"),
                      new City("TBJ", "Trebinje")
              )
      )
    );

    private final AccountRepository accounts;
    private final CountryRepository countries;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    @Override
    public void run(@Nonnull String... args) throws Exception {
        // Only for testing
        if (!accounts.existsByUsername("manager")) {
            var manager = new Manager("manager@cargosync.rs", "https://picsum.photos/200");
            accounts.save(new Account(
                    "manager", passwordEncoder.encode("manager"), AccountRole.MANAGER, manager, true, true, true
            ));
        }
        if (!accounts.existsByUsername("customer")) {
            var customer = new Customer("customer@cargosync.rs", "https://picsum.photos/200", "Dev", "Customer");
            accounts.save(new Account(
                    "customer", passwordEncoder.encode("1234"), AccountRole.CUSTOMER, customer, true, true, true
            ));
        }

        if (countries.count() == 0) {
            countries.saveAll(builtinCountries);
        }

        seedOrderDemoData();
    }

    private void seedOrderDemoData() {
        if (warehouseRepository.count() > 0) return; // already seeded

        // Reload country/city references after potential save above
        Country serbia  = countries.findById("RS").orElseThrow();
        Country bosnia  = countries.findById("BA").orElseThrow();
        City noviSad  = serbia.getCities().get("NVS");
        City beograd  = serbia.getCities().get("BEG");
        City sarajevo = bosnia.getCities().get("SJJ");

        // ---- Product categories + products ----
        ProductCategory electronics = categoryRepository.findByNameIgnoreCase("Electronics")
                .orElseGet(() -> { var c = new ProductCategory("Electronics"); return categoryRepository.save(c); });
        ProductCategory machinery   = categoryRepository.findByNameIgnoreCase("Machinery")
                .orElseGet(() -> { var c = new ProductCategory("Machinery");   return categoryRepository.save(c); });

        Product laptop  = createProduct("Laptop Pro 15", electronics,  new BigDecimal("1299.99"), 2.1);
        Product monitor = createProduct("4K Monitor 27\"", electronics, new BigDecimal("499.99"),  6.5);
        Product forklift = createProduct("Mini Forklift", machinery,   new BigDecimal("8999.00"), 350.0);

        // ---- Warehouses ----
        Warehouse wh1 = createWarehouse("Novi Sad Warehouse", "Bulevar oslobodjenja 10", noviSad,
                new GeoLocation("45.2671", "19.8335"));
        Warehouse wh2 = createWarehouse("Beograd Warehouse", "Kneza Milosa 50", beograd,
                new GeoLocation("44.8176", "20.4569"));
        Warehouse wh3 = createWarehouse("Sarajevo Warehouse", "Titova 20", sarajevo,
                new GeoLocation("43.8519", "18.3866"));

        // ---- Inventory ----
        inventoryRepository.save(new WarehouseInventory(wh1, laptop,   500L));
        inventoryRepository.save(new WarehouseInventory(wh2, laptop,   300L));
        inventoryRepository.save(new WarehouseInventory(wh1, monitor,  200L));
        inventoryRepository.save(new WarehouseInventory(wh3, monitor,  150L));
        inventoryRepository.save(new WarehouseInventory(wh2, forklift,  20L));
        inventoryRepository.save(new WarehouseInventory(wh3, forklift,  10L));

        // ---- Stub company for testing orders ----
        if (companyRepository.count() == 0) {
            companyRepository.save(new Company("Dev Corp d.o.o.", "Futoška 10, Novi Sad"));
        }
    }

    private Product createProduct(String name, ProductCategory category, BigDecimal price, double weight) {
        return productRepository.findAll().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElseGet(() -> {
                    Product p = new Product();
                    p.setName(name);
                    p.setPrice(price);
                    p.setWeight(weight);
                    p.setCategory(category);
                    p.setForSale(true);
                    p.setImageUrl("https://picsum.photos/seed/" + name.hashCode() + "/400/300");
                    return productRepository.save(p);
                });
    }

    private Warehouse createWarehouse(String name, String address, City city, GeoLocation location) {
        Warehouse wh = new Warehouse();
        wh.setName(name);
        wh.setAddress(address);
        wh.setCity(city);
        wh.setLocation(location);
        return warehouseRepository.save(wh);
    }

}
