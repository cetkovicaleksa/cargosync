package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

public interface ContentStore {

    Resource save(String path, InputStreamSource source);

    Resource get(String path);

    void delete(String path);

}
