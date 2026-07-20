package com.earthy.shop.domain.addon.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.domain.addon.dto.request.AddonCreateRequestDto;
import com.earthy.shop.domain.addon.dto.request.AddonUpdateRequestDto;
import com.earthy.shop.domain.addon.dto.response.AddonResponseDto;
import com.earthy.shop.domain.addon.dto.response.AdminAddonResponseDto;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddonService {

    private final AddonRepository addonRepository;
    private final CartItemRepository cartItemRepository;

    // 고객용 추가상품 목록 조회
    public List<AddonResponseDto> getAddons() {
        return addonRepository.findByActiveTrueAndDeletedFalse()
                .stream()
                .map(AddonResponseDto::from)
                .toList();
    }

    // 관리자용 전체 추가상품 목록 조회
    public PageResponseDto<AdminAddonResponseDto> getAdminAddons(Pageable pageable) {
        return PageResponseDto.from(addonRepository.findByDeletedFalse(pageable)
                .map(AdminAddonResponseDto::from));
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
        Addon addon = findAddon(addonId);

        addon.deactivate();

        return AdminAddonResponseDto.from(addon);
    }

    // 관리자용 추가상품 활성화
    @Transactional
    public AdminAddonResponseDto activateAddon(Long addonId) {
        Addon addon = findAddon(addonId);

        addon.activate();

        return AdminAddonResponseDto.from(addon);
    }

    // 관리자용 추가상품 삭제
    @Transactional
    public void deleteAddon(Long addonId) {
        Addon addon = findAddon(addonId);

        // 삭제 추가상품이 담긴 모든 장바구니 항목 정리
        cartItemRepository.deleteByAddon(addon);

        // 추가상품 소프트 삭제
        addon.delete();
    }

    // 추가상품 재고 차감
    @Transactional
    public void decreaseStock(Long addonId, int quantity) {
        // 추가상품 조회
        Addon addon = getActiveAddon(addonId);

        // 추가상품 재고 차감
        addon.decreaseStock(quantity);
    }

    // 추가상품 재고 검증
    public void validateStock(Long addonId, int quantity) {
        Addon addon = getActiveAddon(addonId);

        if (addon.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK);
        }
    }

    // 추가상품 재고 복구
    @Transactional
    public void increaseStock(Long addonId, int quantity) {
        Addon addon = findAddon(addonId);

        addon.increaseStock(quantity);
    }

    // 활성 추가상품 조회
    public Addon getActiveAddon(Long addonId) {
        return addonRepository.findByIdAndActiveTrueAndDeletedFalse(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));
    }

    // 추가상품 단건 조회
    public Addon findAddon(Long addonId) {
        return addonRepository.findById(addonId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADDON_NOT_FOUND));
    }
}
