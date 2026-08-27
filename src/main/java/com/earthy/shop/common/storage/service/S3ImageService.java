package com.earthy.shop.common.storage.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ImageService {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;
    private static final ImageResizePolicy MAIN_IMAGE_POLICY = new ImageResizePolicy(ResizeMode.LONG_EDGE, 1800);
    private static final ImageResizePolicy DETAIL_IMAGE_POLICY = new ImageResizePolicy(ResizeMode.WIDTH, 1400);
    private static final int MAX_PIXELS = 40_000_000;
    private static final float JPEG_QUALITY = 0.85f;
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

    static {
        ImageIO.setUseCache(false);
    }

    private final ObjectProvider<S3Client> s3ClientProvider;

    @Value("${aws.s3.bucket:}")
    private String bucket;

    @Value("${aws.s3.public-base-url:}")
    private String publicBaseUrl;

    // 상품 대표 이미지 업로드
    public String uploadProductImage(MultipartFile file) {
        return upload(file, "products/main", MAIN_IMAGE_POLICY);
    }

    // 상품 상세 이미지 업로드
    public String uploadProductDetailImage(MultipartFile file) {
        return upload(file, "products/details", DETAIL_IMAGE_POLICY);
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
    private String upload(MultipartFile file, String directory, ImageResizePolicy resizePolicy) {
        validateImageFile(file);

        S3Client s3Client = s3ClientProvider.getIfAvailable();

        if (s3Client == null || bucket.isBlank() || publicBaseUrl.isBlank()) {
            throw new BusinessException(ErrorCode.IMAGE_CONFIG_NOT_FOUND);
        }

        OptimizedImage optimizedImage = optimizeImage(file, resizePolicy);
        String key = directory + "/" + UUID.randomUUID() + optimizedImage.extension();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(optimizedImage.contentType())
                .contentLength((long) optimizedImage.bytes().length)
                .build();

        try {
            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(optimizedImage.bytes())
            );
        } catch (S3Exception | SdkClientException e) {
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }

        return publicBaseUrl.replaceAll("/$", "") + "/" + key;
    }

    // 원본은 저장하지 않고 웹 표시용 이미지로 리사이즈/압축한 결과만 S3에 저장
    private OptimizedImage optimizeImage(MultipartFile file, ImageResizePolicy resizePolicy) {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(file.getInputStream())) {
            if (imageInputStream == null) {
                throw new IOException("Image input stream could not be created.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format.");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, true, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if ((long) width * height > MAX_PIXELS) {
                    throw new IOException("Image pixel count is too large.");
                }

                BufferedImage originalImage = reader.read(0);

                if (originalImage == null) {
                    throw new IOException("Image could not be decoded.");
                }

                BufferedImage optimizedImage = resizeAndNormalize(originalImage, resizePolicy);
                boolean hasAlpha = optimizedImage.getColorModel().hasAlpha();
                String outputFormat = hasAlpha ? "png" : "jpeg";
                byte[] bytes = writeImage(optimizedImage, outputFormat);

                return new OptimizedImage(
                        bytes,
                        hasAlpha ? ".png" : ".jpg",
                        hasAlpha ? "image/png" : "image/jpeg"
                );
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            log.warn("[S3 IMAGE OPTIMIZE FAILED] filename={} | message={}",
                    file.getOriginalFilename(),
                    e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    private BufferedImage resizeAndNormalize(BufferedImage source, ImageResizePolicy resizePolicy) {
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = calculateResizeScale(width, height, resizePolicy);
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        boolean hasAlpha = source.getColorModel().hasAlpha();
        int imageType = hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, imageType);

        Graphics2D graphics = target.createGraphics();

        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (!hasAlpha) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
            }

            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }

        return target;
    }

    private double calculateResizeScale(int width, int height, ImageResizePolicy resizePolicy) {
        if (resizePolicy.mode() == ResizeMode.WIDTH) {
            return width > resizePolicy.maxSize() ? (double) resizePolicy.maxSize() / width : 1.0;
        }

        int longEdge = Math.max(width, height);
        return longEdge > resizePolicy.maxSize() ? (double) resizePolicy.maxSize() / longEdge : 1.0;
    }

    private byte[] writeImage(BufferedImage image, String formatName) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);

        if (!writers.hasNext()) {
            throw new IOException("Image writer not found: " + formatName);
        }

        ImageWriter writer = writers.next();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();

            if ("jpeg".equals(formatName) && writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(JPEG_QUALITY);
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return outputStream.toByteArray();
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

    // 업로드 전 형식 검증용 원본 확장자 추출
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

    private record OptimizedImage(byte[] bytes, String extension, String contentType) {
    }

    private record ImageResizePolicy(ResizeMode mode, int maxSize) {
    }

    private enum ResizeMode {
        LONG_EDGE,
        WIDTH
    }
}
