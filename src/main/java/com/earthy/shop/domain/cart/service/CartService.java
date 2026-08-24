package com.earthy.shop.domain.cart.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.entity.Addon;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.cart.dto.request.CartItemAddRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemAddonQuantityUpdateRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemAddonRequestDto;
import com.earthy.shop.domain.cart.dto.request.CartItemQuantityUpdateRequestDto;
import com.earthy.shop.domain.cart.dto.response.CartItemResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.entity.CartItem;
import com.earthy.shop.domain.cart.entity.CartItemAddon;
import com.earthy.shop.domain.cart.repository.CartItemAddonRepository;
import com.earthy.shop.domain.cart.repository.CartItemRepository;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.product.entity.Product;
import com.earthy.shop.domain.product.entity.ProductSizeOption;
import com.earthy.shop.domain.product.enums.ProductCategory;
import com.earthy.shop.domain.product.service.ProductSizeOptionService;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// 장바구니 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final CartItemAddonRepository cartItemAddonRepository;
    private final MemberService memberService;
    private final ProductService productService;
    private final ProductSizeOptionService productSizeOptionService;
    private final AddonService addonService;
    private final IdempotencyService idempotencyService;

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
    public CartResponseDto addCartItem(
            String email,
            CartItemAddRequestDto requestDto,
            String idempotencyKey
    ) {
        // 멱등성 키 검증
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REQUIRED);
        }

        String apiPath = "/api/cart";

        // 기존 멱등성 키 조회
        IdempotencyKey existingKey = idempotencyService.find(
                email,
                idempotencyKey,
                apiPath
        );

        if (existingKey != null) {
            // 이미 처리 완료된 요청이면 장바구니 수량을 다시 증가시키지 않음
            if (existingKey.isCompleted()) {
                return getCart(email);
            }

            // 아직 처리 중이면 중복 요청 차단
            if (existingKey.isProcessing()) {
                throw new BusinessException(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING);
            }
        }

        // 최초 요청이면 멱등성 키 생성
        IdempotencyKey savedKey = idempotencyService.create(
                email,
                idempotencyKey,
                apiPath
        );

        // 실제 장바구니 상품 담기
        CartResponseDto responseDto = addCartItemInternal(email, requestDto);

        // 장바구니 담기 완료 기록
        idempotencyService.complete(
                savedKey,
                null,
                "장바구니 상품 담기 성공"
        );

        return responseDto;
    }

    // 실제 장바구니 상품 담기
    private CartResponseDto addCartItemInternal(String email, CartItemAddRequestDto requestDto) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 상품 조회
        Product product = productService.getActiveProduct(requestDto.getProductId());

        // 포스터 사이즈 옵션 조회
        ProductSizeOption productSizeOption = resolveProductSizeOption(
                product,
                requestDto.getProductSizeOptionId()
        );

        // 요청 추가상품 목록 조회
        Map<Addon, Integer> requestedAddons = resolveRequestedAddons(requestDto);

        // 동일 상품/사이즈 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findSameCartItems(member, product, productSizeOption)
                .stream()
                .findFirst()
                .orElse(null);

        // 현재 장바구니 수량을 포함한 상품 재고 검증
        validateProductCartStock(
                member,
                product,
                productSizeOption,
                cartItem == null ? requestDto.getQuantity() : cartItem.getQuantity() + requestDto.getQuantity(),
                cartItem
        );

        // 현재 장바구니 수량을 포함한 추가상품 재고 검증
        validateAddonCartStock(member, requestedAddons, null);

        if (cartItem == null) {
            // 신규 장바구니 항목 생성
            cartItem = cartItemRepository.save(new CartItem(
                    member,
                    product,
                    productSizeOption,
                    null,
                    requestDto.getQuantity(),
                    0
            ));
        } else {
            // 기존 장바구니 항목 수량 증가
            cartItem.increaseQuantity(requestDto.getQuantity(), 0);
        }

        requestedAddons.forEach(cartItem::addOrIncreaseAddon);

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

        // 변경 후 장바구니 수량 기준 재고 검증
        validateProductCartStock(
                member,
                cartItem.getProduct(),
                cartItem.getProductSizeOption(),
                requestDto.getQuantity(),
                cartItem
        );

        // 장바구니 항목 수량 변경
        cartItem.updateQuantity(requestDto.getQuantity(), cartItem.getAddonQuantity());

        return getCart(email);
    }

    // 장바구니 추가상품 수량 변경
    @Transactional
    public CartResponseDto updateAddonQuantity(
            String email,
            Long cartItemId,
            Long cartItemAddonId,
            CartItemAddonQuantityUpdateRequestDto requestDto
    ) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 요청 회원의 장바구니 항목 조회
        CartItem cartItem = cartItemRepository.findByIdAndMember(cartItemId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        // 요청 장바구니 추가상품 조회
        CartItemAddon cartItemAddon = cartItemAddonRepository.findByIdAndCartItem(cartItemAddonId, cartItem)
                .orElseThrow(() -> new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (requestDto.getQuantity() > 0) {
            validateAddonCartStock(
                    member,
                    Map.of(cartItemAddon.getAddon(), requestDto.getQuantity()),
                    cartItemAddon
            );
        }

        // 추가상품 수량 변경, 0이면 장바구니 추가상품 제거
        cartItem.updateCartItemAddonQuantity(cartItemAddon, requestDto.getQuantity());

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

    // 요청 추가상품 목록 조회
    private Map<Addon, Integer> resolveRequestedAddons(CartItemAddRequestDto requestDto) {
        Map<Long, Integer> addonQuantities = new LinkedHashMap<>();

        // 복수 추가상품 요청이 있으면 실제 요청 배열만 검증 대상으로 사용
        if (requestDto.getAddons() != null) {
            for (CartItemAddonRequestDto addonRequestDto : requestDto.getAddons()) {
                addonQuantities.merge(addonRequestDto.getAddonId(), addonRequestDto.getQuantity(), Integer::sum);
            }
        } else if (requestDto.getAddonId() != null) {
            // 기존 단일 추가상품 요청은 복수 구조 이전 호환용으로만 처리
            int addonQuantity = requestDto.getAddonQuantity() == null ? 1 : requestDto.getAddonQuantity();
            addonQuantities.merge(requestDto.getAddonId(), addonQuantity, Integer::sum);
        }

        Map<Addon, Integer> requestedAddons = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : addonQuantities.entrySet()) {
            if (entry.getValue() < 1) {
                throw new BusinessException(ErrorCode.INVALID_QUANTITY);
            }

            Addon addon = addonService.getActiveAddon(entry.getKey());
            requestedAddons.put(addon, entry.getValue());
        }

        return requestedAddons;
    }

    // 포스터 사이즈 옵션 계산
    private ProductSizeOption resolveProductSizeOption(Product product, Long productSizeOptionId) {
        // 포스터는 사이즈 선택 필수
        if (product.getCategory() == ProductCategory.POSTER) {
            if (productSizeOptionId == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_REQUIRED);
            }

            return productSizeOptionService.getActiveOption(product, productSizeOptionId);
        }

        // 포스터가 아닌 상품은 사이즈 옵션을 사용하지 않음
        if (productSizeOptionId != null) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_SIZE_OPTION);
        }

        return null;
    }

    // 장바구니 전체 수량 기준 상품 재고 검증
    private void validateProductCartStock(
            Member member,
            Product product,
            ProductSizeOption productSizeOption,
            int productQuantity,
            CartItem targetCartItem
    ) {
        List<CartItem> cartItems = cartItemRepository.findByMember(member);

        // 포스터는 같은 사이즈 옵션 수량만, 엽서는 같은 상품 수량만 합산
        int totalProductQuantity = cartItems.stream()
                .filter(cartItem -> !isSameCartItem(cartItem, targetCartItem))
                .filter(cartItem -> isSameProductStockGroup(cartItem, product, productSizeOption))
                .mapToInt(CartItem::getQuantity)
                .sum() + productQuantity;

        validateProductStock(product, productSizeOption, totalProductQuantity);
    }

    // 장바구니 전체 수량 기준 추가상품 재고 검증
    private void validateAddonCartStock(
            Member member,
            Map<Addon, Integer> requestedAddons,
            CartItemAddon targetCartItemAddon
    ) {
        if (requestedAddons.isEmpty()) {
            return;
        }

        List<CartItem> cartItems = cartItemRepository.findByMember(member);

        for (Map.Entry<Addon, Integer> entry : requestedAddons.entrySet()) {
            Addon addon = entry.getKey();
            int requestedQuantity = entry.getValue();

            // 같은 추가상품이 장바구니 전체에 담긴 수량까지 합산
            int totalAddonQuantity = cartItems.stream()
                    .mapToInt(cartItem -> calculateCartAddonQuantity(cartItem, addon, targetCartItemAddon))
                    .sum() + requestedQuantity;

            addonService.validateStock(addon.getId(), totalAddonQuantity);
        }
    }

    // 장바구니 항목에 담긴 특정 추가상품 수량 계산
    private int calculateCartAddonQuantity(
            CartItem cartItem,
            Addon addon,
            CartItemAddon targetCartItemAddon
    ) {
        int legacyAddonQuantity = 0;

        if (cartItem.getAddon() != null && Objects.equals(cartItem.getAddon().getId(), addon.getId())) {
            legacyAddonQuantity = cartItem.getAddonQuantity();
        }

        int multipleAddonQuantity = cartItem.getCartItemAddons()
                .stream()
                .filter(cartItemAddon -> !isSameCartItemAddon(cartItemAddon, targetCartItemAddon))
                .filter(cartItemAddon -> Objects.equals(cartItemAddon.getAddon().getId(), addon.getId()))
                .mapToInt(CartItemAddon::getQuantity)
                .sum();

        return legacyAddonQuantity + multipleAddonQuantity;
    }

    // 검증 대상 장바구니 항목 제외 여부
    private boolean isSameCartItem(CartItem cartItem, CartItem targetCartItem) {
        return targetCartItem != null && Objects.equals(cartItem.getId(), targetCartItem.getId());
    }

    // 검증 대상 장바구니 추가상품 제외 여부
    private boolean isSameCartItemAddon(CartItemAddon cartItemAddon, CartItemAddon targetCartItemAddon) {
        return targetCartItemAddon != null && Objects.equals(cartItemAddon.getId(), targetCartItemAddon.getId());
    }

    // 같은 재고를 공유하는 장바구니 항목인지 확인
    private boolean isSameProductStockGroup(
            CartItem cartItem,
            Product product,
            ProductSizeOption productSizeOption
    ) {
        if (!Objects.equals(cartItem.getProduct().getId(), product.getId())) {
            return false;
        }

        if (product.getCategory() == ProductCategory.POSTER) {
            return cartItem.getProductSizeOption() != null
                    && productSizeOption != null
                    && Objects.equals(cartItem.getProductSizeOption().getId(), productSizeOption.getId());
        }

        return cartItem.getProductSizeOption() == null;
    }

    // 상품 카테고리별 재고 검증
    private void validateProductStock(
            Product product,
            ProductSizeOption productSizeOption,
            int productQuantity
    ) {
        if (product.getCategory() == ProductCategory.POSTER) {
            if (productSizeOption == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SIZE_OPTION_REQUIRED);
            }

            if (productSizeOption.getStockQuantity() < productQuantity) {
                throw new BusinessException(ErrorCode.OUT_OF_STOCK);
            }

            return;
        }

        productService.validateStock(product.getId(), productQuantity);
    }
}
