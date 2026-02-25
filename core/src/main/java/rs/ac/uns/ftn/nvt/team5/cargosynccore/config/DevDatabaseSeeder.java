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
import rs.ac.uns.ftn.nvt.team5.cargosynccore.manager.service.ManagerService;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.City;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Country;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Customer;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.Manager;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.CountryRepository;

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
            var customer = new Customer("customer@cargosync.rs", "https://picsum.photos/200");
            accounts.save(new Account(
                    "customer", passwordEncoder.encode("customer"), AccountRole.CUSTOMER, customer, true, true, true
            ));
        }

        if (countries.count() == 0) {
            countries.saveAll(builtinCountries);
        }

    }

}
