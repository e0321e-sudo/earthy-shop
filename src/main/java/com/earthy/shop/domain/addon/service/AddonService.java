package com.earthy.shop.domain.addon.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.dto.request.AddonCreateRequestDto;
import com.earthy.shop.domain.addon.dto.request.AddonUpdateRequestDto;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.dto.response.AdminAddonResponseDto;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddonService {

    private final AddonRepository addonRepository;

    // 고객용 추가상품 목록 조회
    public List<AddonResponseDto> getAddons() {
        return addonRepository.findByActiveTrue()
                .stream()
                .map(AddonResponseDto::from)
                .toList();
    }

    // 관리자용 전체 추가상품 목록 조회
    public List<AdminAddonResponseDto> getAdminAddons() {
        return addonRepository.findAll()
                .stream()
                .map(AdminAddonResponseDto::from)
                .toList();
    }

    // 관리자용 추가상품 등록
    @Transactional
    public AdminAddonResponseDto createAddon(AddonCreateRequestDto requestDto) {
        Addon addon = new Addon(
                requestDto.getName(),
                requestDto.getType(),
                requestDto.getPrice(),
                requestDto.getStockQuantity()
        );

        Addon savedAddon = addonRepository.save(addon);

        return AdminAddonResponseDto.from(savedAddon);
    }

    // 관리자용 추가상품 수정
    @Transactional
    public AdminAddonResponseDto updateAddon(Long addonId, AddonUpdateRequestDto requestDto) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));

        addon.update(
                requestDto.getName(),
                requestDto.getType(),
                requestDto.getPrice(),
                requestDto.getStockQuantity()
        );

        return AdminAddonResponseDto.from(addon);
    }

    // 관리자용 추가상품 비활성화
    @Transactional
    public AdminAddonResponseDto deactivateAddon(Long addonId) {
        Addon addon = addonRepository.findById(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));

        addon.deactivate();

        return AdminAddonResponseDto.from(addon);
    }

    // 추가상품 재고 차감
    @Transactional
    public void decreaseStock(Long addonId, int quantity) {
        // 추가상품 조회
        Addon addon = getActiveAddon(addonId);

        // 추가상품 재고 차감
        addon.decreaseStock(quantity);
    }

    // 추가상품 재고 복구
    @Transactional
    public void increaseStock(Long addonId, int quantity) {
        Addon addon = findAddon(addonId);

        addon.increaseStock(quantity);
    }

    // 활성 추가상품 조회
    public Addon getActiveAddon(Long addonId) {
        return addonRepository.findByIdAndActiveTrue(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));
    }

    // 추가상품 단건 조회
    public Addon findAddon(Long addonId) {
        return addonRepository.findById(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));
    }
}
