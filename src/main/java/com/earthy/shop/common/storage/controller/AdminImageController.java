package com.earthy.shop.common.storage.controller;

import com.earthy.shop.common.response.ApiResponseDto;
import com.earthy.shop.common.storage.dto.ImageUploadResponseDto;
import com.earthy.shop.common.storage.service.S3ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/admin/images")
public class AdminImageController {

    private final S3ImageService s3ImageService;

    // 상품 대표 이미지 업로드
    @PostMapping("/products")
    public ResponseEntity<ApiResponseDto<ImageUploadResponseDto>> uploadProductImage(
            @RequestParam MultipartFile image
    ) {
        String imageUrl = s3ImageService.uploadProductImage(image);
        return ResponseEntity.ok(ApiResponseDto.success("이미지 업로드 성공", new ImageUploadResponseDto(imageUrl)));
    }

    // 상품 상세 이미지 업로드
    @PostMapping("/products/detail")
    public ResponseEntity<ApiResponseDto<ImageUploadResponseDto>> uploadProductDetailImage(
            @RequestParam MultipartFile image
    ) {
        String imageUrl = s3ImageService.uploadProductDetailImage(image);
        return ResponseEntity.ok(ApiResponseDto.success("이미지 업로드 성공", new ImageUploadResponseDto(imageUrl)));
    }

    // EARTHY가 업로드한 미사용 상품 이미지 삭제
    @DeleteMapping
    public ResponseEntity<ApiResponseDto<Void>> deleteImage(
            @RequestBody ImageDeleteRequest request
    ) {
        log.info("[ADMIN IMAGE DELETE REQUESTED] imageUrl={}", request.imageUrl());
        s3ImageService.deleteImageIfOwned(request.imageUrl());
        return ResponseEntity.ok(ApiResponseDto.success("이미지 삭제 처리 완료", null));
    }

    public record ImageDeleteRequest(String imageUrl) {
    }
}
