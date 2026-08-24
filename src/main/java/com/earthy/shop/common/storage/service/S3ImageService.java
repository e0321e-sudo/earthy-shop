package com.earthy.shop.common.storage.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ImageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg",
            ".jpeg",
            ".png",
            ".webp"
    );
    private static final Set<String> OWNED_IMAGE_PREFIXES = Set.of(
            "products/main/",
            "products/details/",
            "products/detail/"
    );

    private final ObjectProvider<S3Client> s3ClientProvider;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    // 상품 대표 이미지 업로드
    public String uploadProductImage(MultipartFile file) {
        return upload(file, "products/main");
    }

    // 상품 상세 이미지 업로드
    public String uploadProductDetailImage(MultipartFile file) {
        return upload(file, "products/details");
    }

    // EARTHY가 업로드한 상품 이미지만 안전하게 삭제
    public boolean deleteImageIfOwned(String imageUrl) {
        String key = extractOwnedObjectKey(imageUrl);

        if (key == null) {
            return false;
        }

        S3Client s3Client = s3ClientProvider.getIfAvailable();

        if (s3Client == null || bucket.isBlank()) {
            log.warn("[S3 IMAGE DELETE SKIPPED] reason=config-not-found | imageUrl={}", imageUrl);
            return false;
        }

        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());

            log.info("[S3 IMAGE DELETED] key={}", key);
            return true;
        } catch (S3Exception | SdkClientException e) {
            log.warn("[S3 IMAGE DELETE FAILED] key={} | message={}", key, e.getMessage());
            return false;
        }
    }

    // S3 업로드 후 고객/관리자 화면에서 사용할 공개 URL 반환
    private String upload(MultipartFile file, String directory) {
        validateImageFile(file);

        String key = directory + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        S3Client s3Client = s3ClientProvider.getIfAvailable();

        if (s3Client == null || bucket.isBlank() || publicBaseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_CONFIG_NOT_FOUND);
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException | S3Exception | SdkClientException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return publicBaseUrl.replaceAll("/$", "") + "/" + key;
    }

    // 허용된 이미지 형식과 용량만 업로드 허용
    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.IMAGE_FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        String extension = getExtension(file.getOriginalFilename()).toLowerCase(Locale.ROOT);

        if (contentType == null
                || !ALLOWED_CONTENT_TYPES.contains(contentType)
                || !ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.INVALID_IMAGE_FILE);
        }
    }

    // 원본 확장자를 유지해서 브라우저/관리자 화면에서 파일 식별이 쉽도록 처리
    private String getExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }

        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    // 공개 URL에서 우리 상품 이미지 object key만 추출
    private String extractOwnedObjectKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            log.warn("[S3 IMAGE DELETE SKIPPED] reason=empty-url");
            return null;
        }

        if (publicBaseUrl.isBlank()) {
            log.warn("[S3 IMAGE DELETE SKIPPED] reason=public-base-url-not-found");
            return null;
        }

        String normalizedBaseUrl = publicBaseUrl.replaceAll("/$", "");
        String normalizedImageUrl = imageUrl.trim();

        if (!normalizedImageUrl.startsWith(normalizedBaseUrl + "/")) {
            log.warn("[S3 IMAGE DELETE SKIPPED] reason=external-url | imageUrl={}", normalizedImageUrl);
            return null;
        }

        String key = normalizedImageUrl.substring(normalizedBaseUrl.length() + 1);

        if (OWNED_IMAGE_PREFIXES.stream().noneMatch(key::startsWith)) {
            log.warn("[S3 IMAGE DELETE SKIPPED] reason=unexpected-key | key={}", key);
            return null;
        }

        return key;
    }
}
