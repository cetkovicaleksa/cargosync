package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.auth.repository.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.model.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.repository.*;

import java.util.List;

@Profile("dev")
@RequiredArgsConstructor
@Component
public class DatabaseSeeder implements CommandLineRunner {

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
        // maybe leave countries in production, but get rid of accounts ?

        if (accounts.count() == 0) {
            var superManager = new Manager("admin@cargosync.rs", "https://picsum.photos/200");
            accounts.save(new Account(
                    "admin", passwordEncoder.encode("admin"), AccountRole.MANAGER, superManager, true, true, true
            ));

            var manager = new Manager("manager@cargosync.rs", "https://picsum.photos/200");
            accounts.save(new Account(
                    "manager", passwordEncoder.encode("manager"), AccountRole.MANAGER, manager, true, true, true
            ));

            var customer = new Manager("customer@cargosync.rs", "https://picsum.photos/200");
            accounts.save(new Account(
                    "customer", passwordEncoder.encode("customer"), AccountRole.CUSTOMER, customer, true, true, true
            ));
        }

        if (countries.count() == 0) {
            countries.saveAll(builtinCountries);
        }

    }

}
