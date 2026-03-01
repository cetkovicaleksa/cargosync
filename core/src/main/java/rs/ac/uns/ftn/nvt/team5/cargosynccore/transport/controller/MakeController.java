package rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.controller;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.dto.*;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.transport.service.QueryMakesUC;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = {"/vans/makes", "/vans/manufacturers"}, produces = MediaType.APPLICATION_JSON_VALUE)
class MakeController {

    private final QueryMakesUC queryMakesUC;
    private final ModelMapper modelMapper;

    @GetMapping
    Object getAllMakes() {
        var makes = queryMakesUC.getAllMakes()
                .map(make -> modelMapper.map(make, MakeDto.class));

        return ResponseEntity.ok()
                .cacheControl(CacheControl
                        .maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable()
                )
                .lastModified(Instant.EPOCH)
                .body(makes);
    }

    @GetMapping("/{id:.{3}}")
    Object getAMake(@PathVariable String id) {
        var make = modelMapper.map(queryMakesUC.getMake(id.toUpperCase()), MakeDto.class);

        return ResponseEntity.ok()
                .cacheControl(CacheControl
                        .maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable()
                )
                .eTag("v0")
                .lastModified(Instant.EPOCH)
                .body(make);
    }

    @GetMapping("/{id:.{3}}/models")
    Object getMakeModels(@PathVariable String id) {
        var models = queryMakesUC.getAllMakeModels(id.toUpperCase())
                .map(model -> modelMapper.map(model, ModelDto.class));

        return ResponseEntity.ok()
                .cacheControl(CacheControl
                        .maxAge(365, TimeUnit.DAYS)
                        .cachePublic()
                        .immutable()
                )
                .lastModified(Instant.EPOCH)
                .body(models);
    }

}
