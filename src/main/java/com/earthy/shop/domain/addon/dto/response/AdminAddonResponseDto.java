package com.earthy.shop.domain.addon.dto.response;

import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.enums.AddonType;

import java.time.LocalDateTime;

// 관리자용 추가상품 응답 DTO
public record AdminAddonResponseDto(
        Long id,
        String name,
        AddonType type,
        String typeDescription,
        int price,
        int stockQuantity,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminAddonResponseDto from(Addon addon) {
        return new AdminAddonResponseDto(
                addon.getId(),
                addon.getName(),
                addon.getType(),
                addon.getType().getDescription(),
                addon.getPrice(),
                addon.getStockQuantity(),
                addon.isActive(),
                addon.getCreatedAt(),
                addon.getUpdatedAt()
        );
    }
}