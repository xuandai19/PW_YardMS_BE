package com.yms.yms.infrastructure.storage;

import com.yms.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URI;
import java.util.UUID;

/**
 * Foundation storage: local filesystem.
 * MinIO can replace this implementation without changing controller contracts.
 */
@Service
public class ContractStorageService {

    private static final Logger log = LoggerFactory.getLogger(ContractStorageService.class);

    private final Path root;

    public ContractStorageService(
            @Value("${yms.storage.local-path:./data/contracts}") String localPath) throws IOException {
        this.root = Path.of(localPath).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
        log.info("Contract storage root: {}", this.root);
    }

    public String store(UUID contractId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required", HttpStatus.BAD_REQUEST);
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "contract.bin";
        String safeName = contractId + "-" + UUID.randomUUID() + "-" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = root.resolve(safeName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toUri().toString();
        } catch (IOException e) {
            throw new BusinessException("Unable to store file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public Path resolve(String fileUri) {
        try {
            Path path = Path.of(URI.create(fileUri)).toAbsolutePath().normalize();
            if (!path.startsWith(root) || !Files.isRegularFile(path)) {
                throw new BusinessException("Stored file not found", HttpStatus.NOT_FOUND);
            }
            return path;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Stored file not found", HttpStatus.NOT_FOUND);
        }
    }
}
