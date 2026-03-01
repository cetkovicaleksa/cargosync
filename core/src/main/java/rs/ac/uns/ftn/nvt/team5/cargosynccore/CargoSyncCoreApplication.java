package rs.ac.uns.ftn.nvt.team5.cargosynccore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CargoSyncCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CargoSyncCoreApplication.class, args);
    }

}
