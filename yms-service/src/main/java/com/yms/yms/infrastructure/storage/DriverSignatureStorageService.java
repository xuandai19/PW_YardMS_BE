package com.yms.yms.infrastructure.storage;

import com.yms.common.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
public class DriverSignatureStorageService {
    private static final Logger log = LoggerFactory.getLogger(DriverSignatureStorageService.class);
    private static final long MAX_SIGNATURE_BYTES = 5L * 1024 * 1024;

    private final ContractStorageService localStorage;
    private final boolean minioEnabled;
    private final String bucket;
    private final MinioClient minio;

    public DriverSignatureStorageService(ContractStorageService localStorage,
            @Value("${yms.storage.minio.enabled:false}") boolean minioEnabled,
            @Value("${yms.storage.minio.endpoint:http://localhost:9000}") String endpoint,
            @Value("${yms.storage.minio.access-key:yms-minio}") String accessKey,
            @Value("${yms.storage.minio.secret-key:yms-minio-change-me}") String secretKey,
            @Value("${yms.storage.minio.bucket:yms-signatures}") String bucket) {
        this.localStorage = localStorage;
        this.minioEnabled = minioEnabled;
        this.bucket = bucket;
        this.minio = minioEnabled ? MinioClient.builder().endpoint(endpoint)
                .credentials(accessKey, secretKey).build() : null;
    }

    public String store(UUID confirmationId, MultipartFile file) {
        validate(file);
        if (!minioEnabled) {
            return localStorage.store(confirmationId, file);
        }
        String contentType = file.getContentType();
        String extension = "image/png".equalsIgnoreCase(contentType) ? ".png" : ".jpg";
        String objectName = confirmationId + "/" + UUID.randomUUID() + extension;
        try {
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minio.putObject(PutObjectArgs.builder().bucket(bucket).object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(contentType).build());
            return "minio://" + bucket + "/" + objectName;
        } catch (Exception exception) {
            log.error("Unable to upload driver signature to MinIO", exception);
            throw new BusinessException("Unable to store driver signature", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Signature image is required", HttpStatus.BAD_REQUEST);
        }
        String type = file.getContentType();
        if (!"image/png".equalsIgnoreCase(type) && !"image/jpeg".equalsIgnoreCase(type)) {
            throw new BusinessException("Signature must be a PNG or JPEG image", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIGNATURE_BYTES) {
            throw new BusinessException("Signature image must not exceed 5 MB", HttpStatus.BAD_REQUEST);
        }
    }
}
