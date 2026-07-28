package com.earthy.shop.domain.cart.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.enums.AddonType;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.cart.dto.request.CartItemAddRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemQuantityUpdateRequestDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.entity.CartItem;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.service.ProductService;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private ProductService productService;

    @Mock
    private AddonService addonService;

    @InjectMocks
    private CartService cartService;

    @Test
    void 장바구니에_새_상품을_담는다() {
        // given
        Member member = member();
        Product product = product(10);
        CartItem savedCartItem = cartItem(member, product, null, 2, 0);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, null, null, 2);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(cartItemRepository.findByMemberAndProductAndAddon(member, product, null))
                .willReturn(Optional.empty());
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of())
                .willReturn(List.of(savedCartItem));
        given(cartItemRepository.save(any(CartItem.class))).willReturn(savedCartItem);

        // when
        CartResponseDto response = cartService.addCartItem("test@example.com", requestDto);

        // then
        verify(productService).validateStock(1L, 2);
        verify(cartItemRepository).save(any(CartItem.class));
        assertThat(response.items()).hasSize(1);
        assertThat(response.totalPrice()).isEqualTo(7000);
    }

    @Test
    void 같은_상품이_있으면_수량을_증가한다() {
        // given
        Member member = member();
        Product product = product(10);
        CartItem cartItem = cartItem(member, product, null, 1, 0);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, null, null, 2);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(cartItemRepository.findByMemberAndProductAndAddon(member, product, null))
                .willReturn(Optional.of(cartItem));
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of(cartItem))
                .willReturn(List.of(cartItem));

        // when
        CartResponseDto response = cartService.addCartItem("test@example.com", requestDto);

        // then
        verify(productService).validateStock(1L, 3);
        assertThat(cartItem.getQuantity()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualTo(10500);
    }

    @Test
    void 추가상품_수량이_1보다_작으면_예외가_발생한다() {
        // given
        Member member = member();
        Product product = product(10);
        Addon addon = addon(5);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, 1L, 0, 1);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(addonService.getActiveAddon(1L)).willReturn(addon);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> cartService.addCartItem("test@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_QUANTITY)
                );
    }

    @Test
    void 추가상품이_있는_상품을_장바구니에_담는다() {
        // given
        Member member = member();
        Product product = product(10);
        Addon addon = addon(5);
        CartItem savedCartItem = cartItem(member, product, addon, 1, 2);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, 1L, 2, 1);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(addonService.getActiveAddon(1L)).willReturn(addon);
        given(cartItemRepository.findByMemberAndProductAndAddon(member, product, addon))
                .willReturn(Optional.empty());
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of())
                .willReturn(List.of(savedCartItem));
        given(cartItemRepository.save(any(CartItem.class))).willReturn(savedCartItem);

        // when
        CartResponseDto response = cartService.addCartItem("test@example.com", requestDto);

        // then
        verify(productService).validateStock(1L, 1);
        verify(addonService).validateStock(1L, 2);
        assertThat(response.totalPrice()).isEqualTo(27500);
    }

    @Test
    void 같은_상품이지만_추가상품이_다르면_별도_항목으로_담는다() {
        // given
        Member member = member();
        Product product = product(10);
        Addon existingAddon = addon(5);
        Addon newAddon = new Addon("A2 원목 액자", AddonType.FRAME, 15000, 5);
        TestEntityUtils.setId(newAddon, 2L);
        CartItem existingCartItem = cartItem(member, product, existingAddon, 1, 1);
        CartItem savedCartItem = cartItem(member, product, newAddon, 1, 1);
        TestEntityUtils.setId(savedCartItem, 2L);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, 2L, 1, 1);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(addonService.getActiveAddon(2L)).willReturn(newAddon);
        given(cartItemRepository.findByMemberAndProductAndAddon(member, product, newAddon))
                .willReturn(Optional.empty());
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of(existingCartItem))
                .willReturn(List.of(existingCartItem, savedCartItem));
        given(cartItemRepository.save(any(CartItem.class))).willReturn(savedCartItem);

        // when
        CartResponseDto response = cartService.addCartItem("test@example.com", requestDto);

        // then
        verify(cartItemRepository).save(any(CartItem.class));
        assertThat(response.items()).hasSize(2);
    }

    @Test
    void 장바구니_상품_수량을_변경한다() {
        // given
        Member member = member();
        Product product = product(10);
        CartItem cartItem = cartItem(member, product, null, 1, 0);
        CartItemQuantityUpdateRequestDto requestDto = new CartItemQuantityUpdateRequestDto(3, null);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartItemRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of(cartItem))
                .willReturn(List.of(cartItem));

        // when
        CartResponseDto response = cartService.updateQuantity("test@example.com", 1L, requestDto);

        // then
        verify(productService).validateStock(1L, 3);
        assertThat(cartItem.getQuantity()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualTo(10500);
    }

    @Test
    void 장바구니_추가상품_수량을_변경한다() {
        // given
        Member member = member();
        Product product = product(10);
        Addon addon = addon(10);
        CartItem cartItem = cartItem(member, product, addon, 1, 1);
        CartItemQuantityUpdateRequestDto requestDto = new CartItemQuantityUpdateRequestDto(1, 3);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartItemRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.findByMember(member))
                .willReturn(List.of(cartItem))
                .willReturn(List.of(cartItem));

        // when
        CartResponseDto response = cartService.updateQuantity("test@example.com", 1L, requestDto);

        // then
        verify(addonService).validateStock(1L, 3);
        assertThat(cartItem.getAddonQuantity()).isEqualTo(3);
        assertThat(response.totalPrice()).isEqualTo(39500);
    }

    @Test
    void 장바구니_상품을_삭제한다() {
        // given
        Member member = member();
        Product product = product(10);
        CartItem cartItem = cartItem(member, product, null, 1, 0);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartItemRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(cartItem));
        given(cartItemRepository.findByMember(member)).willReturn(List.of());

        // when
        CartResponseDto response = cartService.deleteCartItem("test@example.com", 1L);

        // then
        verify(cartItemRepository).delete(cartItem);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void 장바구니를_전체_삭제한다() {
        // given
        Member member = member();

        given(memberService.getActiveMember("test@example.com")).willReturn(member);

        // when
        cartService.clearCart("test@example.com");

        // then
        verify(cartItemRepository).deleteByMember(member);
    }

    @Test
    void 선택한_장바구니_항목을_삭제한다() {
        // given
        Member member = member();
        Product product = product(10);
        CartItem firstItem = cartItem(member, product, null, 1, 0);
        CartItem secondItem = cartItem(member, product, null, 2, 0);
        TestEntityUtils.setId(secondItem, 2L);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartItemRepository.findByIdAndMember(1L, member)).willReturn(Optional.of(firstItem));
        given(cartItemRepository.findByIdAndMember(2L, member)).willReturn(Optional.of(secondItem));

        // when
        cartService.deleteCartItems("test@example.com", List.of(1L, 2L));

        // then
        verify(cartItemRepository).delete(firstItem);
        verify(cartItemRepository).delete(secondItem);
    }

    @Test
    void 장바구니_전체_수량이_재고보다_많으면_예외가_발생한다() {
        // given
        Member member = member();
        Product product = product(2);
        CartItemAddRequestDto requestDto = new CartItemAddRequestDto(1L, null, null, 3);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(productService.getActiveProduct(1L)).willReturn(product);
        given(cartItemRepository.findByMemberAndProductAndAddon(member, product, null))
                .willReturn(Optional.empty());
        given(cartItemRepository.findByMember(member)).willReturn(List.of());
        doThrow(new BusinessException(ErrorCode.OUT_OF_STOCK))
                .when(productService)
                .validateStock(1L, 3);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> cartService.addCartItem("test@example.com", requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.OUT_OF_STOCK)
                );
    }

    @Test
    void 없는_장바구니_항목_수정_시_예외가_발생한다() {
        // given
        Member member = member();
        CartItemQuantityUpdateRequestDto requestDto = new CartItemQuantityUpdateRequestDto(2, null);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartItemRepository.findByIdAndMember(1L, member)).willReturn(Optional.empty());

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> cartService.updateQuantity("test@example.com", 1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND)
                );
    }

    private Member member() {
        Member member = new Member("test@example.com", "encodedPassword", "홍길동", "010-1234-5678");
        TestEntityUtils.setId(member, 1L);
        return member;
    }

    private Product product(int stockQuantity) {
        Product product = new Product(
                "sunset sea postcard",
                ProductCategory.POSTCARD,
                3500,
                "/assets/products/sunset-sea.jpeg",
                "/assets/products/detail.jpeg",
                "노을이 담긴 엽서",
                stockQuantity
        );
        TestEntityUtils.setId(product, 1L);
        return product;
    }

    private Addon addon(int stockQuantity) {
        Addon addon = new Addon("A3 원목 액자", AddonType.FRAME, 12000, stockQuantity);
        TestEntityUtils.setId(addon, 1L);
        return addon;
    }

    private CartItem cartItem(Member member, Product product, Addon addon, int quantity, int addonQuantity) {
        CartItem cartItem = new CartItem(member, product, addon, quantity, addonQuantity);
        TestEntityUtils.setId(cartItem, 1L);
        return cartItem;
    }
}
