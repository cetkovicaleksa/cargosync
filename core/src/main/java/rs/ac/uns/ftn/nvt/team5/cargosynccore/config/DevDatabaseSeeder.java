package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.Account;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.AccountRole;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.AccountRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.model.Company;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.company.repository.CompanyRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.product.repository.ProductRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.ClimateZone;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.Warehouse;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseInventory;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseSector;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.model.WarehouseStatus;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseInventoryRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseSectorRepository;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.warehouse.repository.WarehouseStatusRepository;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
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
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompanyRepository companyRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseStatusRepository warehouseStatusRepository;
    private final WarehouseSectorRepository warehouseSectorRepository;
    private final WarehouseInventoryRepository inventoryRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;

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
        // Load cities directly from DB to avoid any Hibernate first-level cache issues
        // with composite FK resolution (City has @IdClass with Country + code)
        City noviSad  = cityRepository.findByCountryCodeAndCode("RS", "NVS").orElseThrow();
        City beograd  = cityRepository.findByCountryCodeAndCode("RS", "BEG").orElseThrow();
        City sarajevo = cityRepository.findByCountryCodeAndCode("BA", "SJJ").orElseThrow();

        if (warehouseRepository.count() > 0) {
            // no-op: warehouses already seeded
        } else {
            // ---- Product categories + products ----
            ProductCategory electronics = categoryRepository.findByNameIgnoreCase("Electronics")
                    .orElseGet(() -> { var c = new ProductCategory("Electronics"); return categoryRepository.save(c); });
            ProductCategory machinery   = categoryRepository.findByNameIgnoreCase("Machinery")
                    .orElseGet(() -> { var c = new ProductCategory("Machinery");   return categoryRepository.save(c); });

            Product laptop   = createProduct("Laptop Pro 15",   electronics, new BigDecimal("1299.99"), 2.1);
            Product monitor  = createProduct("4K Monitor 27\"",  electronics, new BigDecimal("499.99"),  6.5);
            Product forklift = createProduct("Mini Forklift",    machinery,   new BigDecimal("8999.00"), 350.0);

            // ---- Warehouses ----
            Warehouse wh1 = createWarehouse("Novi Sad Warehouse", "Bulevar oslobodjenja 10", noviSad,
                    new GeoLocation("45.2671", "19.8335"));
            Warehouse wh2 = createWarehouse("Beograd Warehouse", "Kneza Milosa 50", beograd,
                    new GeoLocation("44.8176", "20.4569"));
            Warehouse wh3 = createWarehouse("Sarajevo Warehouse", "Titova 20", sarajevo,
                    new GeoLocation("43.8519", "18.3866"));

            // ---- Inventory ----
            inventoryRepository.save(new WarehouseInventory(wh1, laptop,    500L));
            inventoryRepository.save(new WarehouseInventory(wh2, laptop,    300L));
            inventoryRepository.save(new WarehouseInventory(wh1, monitor,   200L));
            inventoryRepository.save(new WarehouseInventory(wh3, monitor,   150L));
            inventoryRepository.save(new WarehouseInventory(wh2, forklift,   20L));
            inventoryRepository.save(new WarehouseInventory(wh3, forklift,   10L));
        }

        // ---- Simulator test warehouse ----
        Warehouse simWh = warehouseRepository.findByName("Simulator Test Warehouse")
                .orElseGet(() -> createWarehouse("Simulator Test Warehouse", "Bulevar Evrope 1", noviSad,
                        new GeoLocation("45.2550", "19.8450")));
        if (warehouseSectorRepository.findByWarehouseId(simWh.getId()).isEmpty()) {
            addSector(simWh, "Frozen",       ClimateZone.FROZEN);
            addSector(simWh, "Refrigerated", ClimateZone.REFRIGERATED);
            addSector(simWh, "Ambient",      ClimateZone.AMBIENT);
        }
        log.info("=== SIMULATOR TEST WAREHOUSE ID: {} ===", simWh.getId());

        // ---- Companies (always ensure these exist) ----
        Customer devCustomer = customerRepository.findAll().stream()
                .filter(c -> "customer@cargosync.rs".equals(c.getEmail()))
                .findFirst().orElse(null);
        if (devCustomer != null) {
            createCompany(devCustomer, "Dev Corp d.o.o.",      "Futoška 10",      noviSad,  "https://picsum.photos/seed/devcorp/800/600");
            createCompany(devCustomer, "TechLogistics d.o.o.", "Kneza Miloša 12", beograd,  "https://picsum.photos/seed/techlog/800/600");
            createCompany(devCustomer, "BalkaTrade d.o.o.",    "Titova 5",        sarajevo, "https://picsum.photos/seed/balkatrade/800/600");
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

    private void createCompany(Customer owner, String name, String address, City city, String imageUrl) {
        if (companyRepository.existsByName(name)) return;
        Company company = new Company();
        company.setOwner(owner);
        company.setName(name);
        company.setAddress(address);
        company.setCity(city);
        company.getImages().add(imageUrl);
        companyRepository.save(company);
    }

    private Warehouse createWarehouse(String name, String address, City city, GeoLocation location) {
        Warehouse wh = new Warehouse();
        wh.setName(name);
        wh.setAddress(address);
        wh.setCity(city);
        wh.setLocation(location);
        var saved = warehouseRepository.save(wh);
        warehouseStatusRepository.save(new WarehouseStatus(saved));
        return saved;
    }

    private void addSector(Warehouse wh, String name, ClimateZone zone) {
        WarehouseSector sector = new WarehouseSector();
        sector.setWarehouse(wh);
        sector.setName(name);
        sector.setClimateZone(zone);
        warehouseSectorRepository.save(sector);
    }

}
