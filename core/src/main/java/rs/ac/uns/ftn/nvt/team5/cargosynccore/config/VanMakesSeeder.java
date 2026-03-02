package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Make;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.Model;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository.MakeRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@Component
public class VanMakesSeeder implements CommandLineRunner {

    private final MakeRepository makes;

    private static final String VAN_MAKES_PATH = "van_makes.yaml";

    @Override
    public void run(@Nullable String... args) throws Exception {
        if (makes.count() == 0) {
            var predefinedMakes = loadMakes(VAN_MAKES_PATH).stream()
                    .map(VanMakesSeeder::parseMake)
                    .toList();

            makes.saveAll(predefinedMakes);
        }
    }

    @SuppressWarnings("all") // just for path always VAN_MAKES_PATH
    private List<YamlMake> loadMakes(String path) {
        var yaml = new Yaml(new Constructor(YamlMake.class, new LoaderOptions()));

        try (var inputStream = this.getClass().getClassLoader().getResourceAsStream(path)) {
            return StreamSupport.stream(yaml.loadAll(inputStream).spliterator(), false)
                    .map(YamlMake.class::cast)
                    .toList();
        } catch (IOException e) {
            log.error("Error while loading yaml makes", e);
            return List.of();
        }
    }

    private static Make parseMake(YamlMake yamlMake) {
        var models = yamlMake.models.entrySet().stream()
                .map(e -> new Model(yamlMake.wmi, e.getKey(), e.getValue().name))
                .toList();

        return new Make(yamlMake.wmi, yamlMake.name, models);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YamlModel {
        String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class YamlMake {
        String wmi, name;
        Map<String, YamlModel> models;
    }

}
