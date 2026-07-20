package com.earthy.shop.domain.cart.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.cart.dto.request.CartItemAddRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemQuantityUpdateRequestDto;
import com.earthy.shop.domain.cart.dto.response.CartItemResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.entity.CartItem;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

// 장바구니 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final MemberService memberService;
    private final ProductService productService;
    private final AddonService addonService;

    // 장바구니 조회
    public CartResponseDto getCart(String email) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        List<CartItemResponseDto> items = cartItemRepository.findByMember(member)
                .stream()
                .map(CartItemResponseDto::from)
                .toList();

        return CartResponseDto.from(items);
    }

    // 장바구니 상품 담기
    @Transactional
    public CartResponseDto addCartItem(String email, CartItemAddRequestDto requestDto) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 상품 조회
        Product product = productService.getActiveProduct(requestDto.getProductId());

        Addon addon = null;

        // 요청 추가상품 조회
        if (requestDto.getAddonId() != null) {
            addon = addonService.getActiveAddon(requestDto.getAddonId());
        }

        // 추가상품 수량 검증
        int addonQuantity = resolveAddonQuantity(addon, requestDto.getAddonQuantity());

        // 동일 상품 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findByMemberAndProductAndAddon(member, product, addon)
                .orElse(null);

        // 현재 장바구니 수량을 포함한 재고 검증
        validateCartStock(
                member,
                product,
                cartItem == null ? requestDto.getQuantity() : cartItem.getQuantity() + requestDto.getQuantity(),
                addon,
                cartItem == null ? addonQuantity : cartItem.getAddonQuantity() + addonQuantity,
                cartItem
        );

        if (cartItem == null) {
            // 신규 장바구니 항목 생성
            cartItemRepository.save(new CartItem(
                    member,
                    product,
                    addon,
                    requestDto.getQuantity(),
                    addonQuantity
            ));
        } else {
            // 기존 장바구니 항목 수량 증가
            cartItem.increaseQuantity(requestDto.getQuantity(), addonQuantity);
        }

        return getCart(email);
    }

    // 장바구니 수량 변경
    @Transactional
    public CartResponseDto updateQuantity(
            String email,
            Long cartItemId,
            CartItemQuantityUpdateRequestDto requestDto
    ) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 회원의 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findByIdAndMember(cartItemId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 추가상품 수량 검증
        int addonQuantity = resolveUpdateAddonQuantity(cartItem, requestDto.getAddonQuantity());

        // 변경 후 장바구니 수량 기준 재고 검증
        validateCartStock(
                member,
                cartItem.getProduct(),
                requestDto.getQuantity(),
                cartItem.getAddon(),
                addonQuantity,
                cartItem
        );

        // 장바구니 항목 수량 변경
        cartItem.updateQuantity(requestDto.getQuantity(), addonQuantity);

        return getCart(email);
    }

    // 장바구니 항목 삭제
    @Transactional
    public CartResponseDto deleteCartItem(String email, Long cartItemId) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 회원의 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findByIdAndMember(cartItemId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 장바구니 항목 삭제
        cartItemRepository.delete(cartItem);

        return getCart(email);
    }

    // 장바구니 전체 삭제
    @Transactional
    public void clearCart(String email) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 회원 장바구니 전체 삭제
        cartItemRepository.deleteByMember(member);
    }

    // 선택 장바구니 항목 삭제
    @Transactional
    public void deleteCartItems(String email, List<Long> cartItemIds) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        for (Long cartItemId : cartItemIds) {
            // 요청 회원의 장바구니 항목 조회
            CartItem cartItem = cartItemRepository.findByIdAndMember(cartItemId, member)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

            // 장바구니 항목 삭제
            cartItemRepository.delete(cartItem);
        }
    }

    // 추가상품 수량 계산
    private int resolveAddonQuantity(Addon addon, Integer addonQuantity) {
        // 추가상품 미선택
        if (addon == null) {
            return 0;
        }

        // 추가상품 선택 후 수량 미입력
        if (addonQuantity == null) {
            return 1;
        }

        // 추가상품 수량 오류
        if (addonQuantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        return addonQuantity;
    }

    // 추가상품 변경 수량 계산
    private int resolveUpdateAddonQuantity(CartItem cartItem, Integer addonQuantity) {
        // 추가상품 없는 장바구니 항목
        if (cartItem.getAddon() == null) {
            return 0;
        }

        // 추가상품 수량 미변경
        if (addonQuantity == null) {
            return cartItem.getAddonQuantity();
        }

        // 추가상품 수량 오류
        if (addonQuantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }

        return addonQuantity;
    }

    // 장바구니 전체 수량 기준 재고 검증
    private void validateCartStock(
            Member member,
            Product product,
            int productQuantity,
            Addon addon,
            int addonQuantity,
            CartItem targetCartItem
    ) {
        List<CartItem> cartItems = cartItemRepository.findByMember(member);

        // 같은 상품이 다른 옵션으로 담긴 수량까지 합산
        int totalProductQuantity = cartItems.stream()
                .filter(cartItem -> !isSameCartItem(cartItem, targetCartItem))
                .filter(cartItem -> Objects.equals(cartItem.getProduct().getId(), product.getId()))
                .mapToInt(CartItem::getQuantity)
                .sum() + productQuantity;

        productService.validateStock(product.getId(), totalProductQuantity);

        if (addon == null) {
            return;
        }

        // 같은 추가상품이 다른 장바구니 항목에 담긴 수량까지 합산
        int totalAddonQuantity = cartItems.stream()
                .filter(cartItem -> !isSameCartItem(cartItem, targetCartItem))
                .filter(cartItem -> cartItem.getAddon() != null)
                .filter(cartItem -> Objects.equals(cartItem.getAddon().getId(), addon.getId()))
                .mapToInt(CartItem::getAddonQuantity)
                .sum() + addonQuantity;

        addonService.validateStock(addon.getId(), totalAddonQuantity);
    }

    // 검증 대상 장바구니 항목 제외 여부
    private boolean isSameCartItem(CartItem cartItem, CartItem targetCartItem) {
        return targetCartItem != null && Objects.equals(cartItem.getId(), targetCartItem.getId());
    }
}
