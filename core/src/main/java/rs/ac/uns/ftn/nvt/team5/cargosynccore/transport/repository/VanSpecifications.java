package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.repository;

import jakarta.annotation.Nullable;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.model.*;

import java.util.Collection;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class VanSpecifications {

    public static Specification<Van> makeIn(Collection<String> makes) {
        return (root, query, cb) ->
                makes == null || makes.isEmpty()
                        ? null
                        : root.get("wmi").in(makes.stream().map(String::toUpperCase).toList());
    }

    public static Specification<Van> modelIn(Collection<String> models) {
        return (root, query, cb) ->
                models == null || models.isEmpty()
                        ? null
                        : root.get("vd").in(models.stream().map(String::toUpperCase).toList());
    }

    public static Specification<Van> maxPayloadGte(Integer weightKg) {
        return (root, query, cb) ->
                weightKg == null
                        ? null
                        : cb.greaterThanOrEqualTo(
                                root.get("maxPayload"), weightKg
                        );
    }

    public static Specification<Van> maxPayloadLte(Integer weightKg) {
        return (root, query, cb) ->
                weightKg == null
                        ? null
                        : cb.lessThanOrEqualTo(
                        root.get("maxPayload"), weightKg
                );
    }

    public static Specification<Van> matchesQuery(@Nullable String query) {
        return (root, cq, cb) -> {
            if (!StringUtils.hasText(query))
                return null;

            String pattern = "%" + query.toLowerCase() + "%";

            var modelJoin = root.join("model");
            var makeJoin = modelJoin.join("make");

            var modelName = cb.lower(modelJoin.get("name"));
            var makeName = cb.lower(makeJoin.get("name"));

            return cb.or(
                    cb.like(modelName, pattern),
                    cb.like(makeName, pattern)
            );
        };
    }

}
