package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartItemResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.service.CartService;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.order.dto.request.OrderCreateRequestDto;
import com.earthy.shop.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.order.repository.OrderRepository;
import com.earthy.shop.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final CartService cartService;
    private final DeliveryFeeCalculator deliveryFeeCalculator;
    private final ProductService productService;
    private final AddonService addonService;

    // 주문 생성
    @Transactional
    public OrderResponseDto createOrder(String email, OrderCreateRequestDto requestDto) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 장바구니 조회
        CartResponseDto cart = cartService.getCart(email);

        // 장바구니 검증
        if (cart.items().isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART);
        }

        // 주문 대상 장바구니 상품 조회
        List<CartItemResponseDto> orderItems = resolveOrderItems(cart, requestDto.getCartItemIds());

        // 주문 대상 상품 검증
        if (orderItems.isEmpty()) {
            throw new BusinessException(ErrorCode.EMPTY_CART);
        }

        // 주문 생성 직전 현재 재고 검증
        validateOrderStock(orderItems);

        // 상품 총 금액 계산
        int productTotalPrice = orderItems.stream()
                .mapToInt(CartItemResponseDto::itemTotalPrice)
                .sum();

        // 기본 배송비 계산
        int deliveryFee = deliveryFeeCalculator.calculateBaseDeliveryFee(productTotalPrice);

        // 지역별 배송비 계산
        int remoteAreaDeliveryFee = deliveryFeeCalculator.calculateRemoteAreaDeliveryFee(
                requestDto.getZipCode(),
                requestDto.getAddress()
        );

        // 주문 생성
        Order order = new Order(
                createOrderNumber(),
                member,
                requestDto.getReceiverName(),
                requestDto.getReceiverPhone(),
                requestDto.getZipCode(),
                requestDto.getAddress(),
                requestDto.getDetailAddress(),
                requestDto.getDeliveryMemo(),
                productTotalPrice,
                deliveryFee,
                remoteAreaDeliveryFee
        );

        // 주문 상품 생성
        for (CartItemResponseDto cartItem : orderItems) {
            OrderItem orderItem = new OrderItem(
                    cartItem.productId(),
                    cartItem.productName(),
                    cartItem.productImageUrl(),
                    cartItem.productPrice(),
                    cartItem.addonId(),
                    cartItem.addonName(),
                    cartItem.addonPrice(),
                    cartItem.addonQuantity(),
                    cartItem.quantity()
            );

            order.addOrderItem(orderItem);
        }

        // 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 주문 완료 후 장바구니 정리
        clearOrderedCartItems(email, requestDto.getCartItemIds());

        return OrderResponseDto.from(savedOrder);
    }

    // 주문 대상 장바구니 상품 조회
    private List<CartItemResponseDto> resolveOrderItems(
            CartResponseDto cart,
            List<Long> cartItemIds
    ) {
        // 전체상품주문
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            return cart.items();
        }

        Set<Long> selectedCartItemIds = Set.copyOf(cartItemIds);

        List<CartItemResponseDto> orderItems = cart.items()
                .stream()
                .filter(item -> selectedCartItemIds.contains(item.cartItemId()))
                .toList();

        // 선택한 장바구니 항목이 현재 회원 장바구니에 모두 존재하는지 검증
        Set<Long> foundCartItemIds = orderItems.stream()
                .map(CartItemResponseDto::cartItemId)
                .collect(Collectors.toSet());

        if (!foundCartItemIds.containsAll(selectedCartItemIds)) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        return orderItems;
    }

    // 주문 상품 재고 검증
    private void validateOrderStock(List<CartItemResponseDto> orderItems) {
        Map<Long, Integer> productQuantities = new HashMap<>();
        Map<Long, Integer> addonQuantities = new HashMap<>();

        // 같은 상품과 추가상품이 여러 장바구니 항목에 나뉜 경우 합산
        for (CartItemResponseDto orderItem : orderItems) {
            productQuantities.merge(orderItem.productId(), orderItem.quantity(), Integer::sum);

            if (orderItem.addonId() != null) {
                addonQuantities.merge(orderItem.addonId(), orderItem.addonQuantity(), Integer::sum);
            }
        }

        productQuantities.forEach(productService::validateStock);
        addonQuantities.forEach(addonService::validateStock);
    }

    // 주문 완료 후 장바구니 정리
    private void clearOrderedCartItems(String email, List<Long> cartItemIds) {
        // 전체상품주문 장바구니 전체 삭제
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            cartService.clearCart(email);
            return;
        }

        // 선택상품주문 장바구니 선택 삭제
        cartService.deleteCartItems(email, cartItemIds);
    }

    // 주문번호 생성
    private String createOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + date + "-" + random;
    }

    // 내 주문 목록 조회
    public PageResponseDto<OrderResponseDto> getMyOrders(String email, Pageable pageable) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        return PageResponseDto.from(orderRepository.findByMember(member, pageable)
                .map(OrderResponseDto::from));
    }

    // 내 주문 상세 조회
    public OrderResponseDto getMyOrder(String email, Long orderId) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        // 회원 주문 조회
        Order order = orderRepository.findByIdAndMember(orderId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        return OrderResponseDto.from(order);
    }

    // 내 주문 단건 조회 (취소 처리용 Order 엔티티 반환)
    public Order findMyOrder(String email, Long orderId) {
        Member member = memberService.getActiveMember(email);

        return orderRepository.findByIdAndMember(orderId, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 관리자 주문 목록 조회
    public PageResponseDto<OrderResponseDto> getOrders(Pageable pageable) {
        return PageResponseDto.from(orderRepository.findAll(pageable)
                .map(OrderResponseDto::from));
    }

    // 관리자 주문 상세 조회
    public OrderResponseDto getOrderDetail(Long orderId) {
        // 주문 조회
        Order order = getOrder(orderId);

        return OrderResponseDto.from(order);
    }

    // 주문 조회
    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 주문 결제 완료 처리
    @Transactional
    public void payOrder(Order order, String paymentMethod) {
        order.pay(paymentMethod);
    }

    // 관리자 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateRequestDto requestDto) {
        // 주문 조회
        Order order = getOrder(orderId);

        // 배송 정보 등록
        if (requestDto.getStatus() == OrderStatus.SHIPPED) {
            order.registerShipment(
                    requestDto.getCarrier(),
                    requestDto.getTrackingNumber()
            );
        }

        // 주문 상태 변경
        order.updateStatus(requestDto.getStatus());

        return OrderResponseDto.from(order);
    }
}
