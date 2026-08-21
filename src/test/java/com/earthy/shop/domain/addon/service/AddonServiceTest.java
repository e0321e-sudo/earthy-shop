package com.earthy.shop.domain.addon.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.dto.request.AddonCreateRequestDto;
import com.earthy.shop.domain.addon.dto.request.AddonUpdateRequestDto;
import com.earthy.shop.domain.addon.dto.response.AdminAddonResponseDto;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.enums.AddonType;
import com.earthy.shop.domain.addon.repository.AddonRepository;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AddonServiceTest {

    @Mock
    private AddonRepository addonRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private AddonService addonService;

    @Test
    void 추가상품을_등록한다() {
        // given
        AddonCreateRequestDto requestDto = new AddonCreateRequestDto(
                "A3 원목 액자",
                AddonType.FRAME,
                12000,
                10
        );

        given(addonRepository.save(any(Addon.class)))
                .willAnswer(invocation -> {
                    Addon addon = invocation.getArgument(0);
                    TestEntityUtils.setId(addon, 1L);
                    return addon;
                });

        // when
        AdminAddonResponseDto response = addonService.createAddon(requestDto);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("A3 원목 액자");
        assertThat(response.stockQuantity()).isEqualTo(10);
    }

    @Test
    void 추가상품을_수정한다() {
        // given
        Addon addon = addon(10);
        AddonUpdateRequestDto requestDto = new AddonUpdateRequestDto(
                "A2 원목 액자",
                AddonType.FRAME,
                15000,
                20
        );

        given(addonRepository.findById(1L)).willReturn(Optional.of(addon));

        // when
        AdminAddonResponseDto response = addonService.updateAddon(1L, requestDto);

        // then
        assertThat(response.name()).isEqualTo("A2 원목 액자");
        assertThat(response.price()).isEqualTo(15000);
        assertThat(response.stockQuantity()).isEqualTo(20);
    }

    @Test
    void 추가상품_재고를_차감한다() {
        // given
        Addon addon = addon(5);

        given(addonRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(addon));

        // when
        addonService.decreaseStock(1L, 2);

        // then
        verify(addonRepository).findActiveByIdForUpdate(1L);
        assertThat(addon.getStockQuantity()).isEqualTo(3);
    }

    @Test
    void 추가상품_재고가_부족하면_예외가_발생한다() {
        // given
        Addon addon = addon(1);

        given(addonRepository.findActiveByIdForUpdate(1L)).willReturn(Optional.of(addon));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> addonService.decreaseStock(1L, 2))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK)
                );
        verify(addonRepository).findActiveByIdForUpdate(1L);
    }

    @Test
    void 추가상품_재고를_복구한다() {
        // given
        Addon addon = addon(5);

        given(addonRepository.findById(1L)).willReturn(Optional.of(addon));

        // when
        addonService.increaseStock(1L, 3);

        // then
        assertThat(addon.getStockQuantity()).isEqualTo(8);
    }

    @Test
    void 추가상품을_판매중지한다() {
        // given
        Addon addon = addon(5);

        given(addonRepository.findById(1L)).willReturn(Optional.of(addon));

        // when
        AdminAddonResponseDto response = addonService.deactivateAddon(1L);

        // then
        assertThat(response.active()).isFalse();
    }

    @Test
    void 추가상품을_판매재개한다() {
        // given
        Addon addon = addon(5);
        addon.deactivate();

        given(addonRepository.findById(1L)).willReturn(Optional.of(addon));

        // when
        AdminAddonResponseDto response = addonService.activateAddon(1L);

        // then
        assertThat(response.active()).isTrue();
    }

    @Test
    void 추가상품을_삭제하면_장바구니_항목을_정리하고_소프트_삭제한다() {
        // given
        Addon addon = addon(5);

        given(addonRepository.findById(1L)).willReturn(Optional.of(addon));

        // when
        addonService.deleteAddon(1L);

        // then
        verify(cartItemRepository).deleteByAddon(addon);
        assertThat(addon.isDeleted()).isTrue();
        assertThat(addon.isActive()).isFalse();
    }

    @Test
    void 추가상품이_없으면_예외가_발생한다() {
        // given
        given(addonRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> addonService.findAddon(1L))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ADDON_NOT_FOUND)
                );
    }

    private Addon addon(int stockQuantity) {
        Addon addon = new Addon("A3 원목 액자", AddonType.FRAME, 12000, stockQuantity);
        TestEntityUtils.setId(addon, 1L);
        return addon;
    }
}
