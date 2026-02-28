package rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.nvt.team5.cargosynccore.shared.exception.NotFoundException;

import java.io.IOException;
import java.nio.file.*;

import static java.nio.file.Files.*;

@Slf4j
@Service
class LocalFileSystemContentStore implements ContentStore {

    @Value("${spring.content.store.location}") // reuse spring content property
    private Path contentRoot;

    @Override
    public Resource save(String path, InputStreamSource source) {
        try {
            var target = contentRoot.resolve(path);
            createDirectories(target.getParent());
            copy(source.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return new FileSystemResource(target);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Resource get(String path) {
        var target = contentRoot.resolve(path);
        if (!exists(target)) {
            throw new NotFoundException("File not found: " + path);
        }

        return new FileSystemResource(target);
    }

    @Override
    public void delete(String path) {
        try {
            Path target = contentRoot.resolve(path);
            deleteIfExists(target);
        } catch (IOException e) {
            log.error(e.getMessage());
            throw new RuntimeException("Failed to delete file: " + path, e);
        }
    }
}
