package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.domain.cart.dto.response.CartItemResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.service.CartService;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.order.dto.request.OrderCreateRequestDto;
import com.earthy.shop.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberService memberService;
    private final CartService cartService;

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
                cart.totalPrice()
        );

        // 주문 상품 생성
        for (CartItemResponseDto cartItem : cart.items()) {
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

        // 장바구니 비우기
        cartService.clearCart(email);

        return OrderResponseDto.from(savedOrder);
    }

    // 주문번호 생성
    private String createOrderNumber() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        return "ORD-" + date + "-" + random;
    }

    // 내 주문 목록 조회
    public List<OrderResponseDto> getMyOrders(String email) {
        // 요청 회원 조회
        Member member = memberService.getActiveMember(email);

        return orderRepository.findByMemberOrderByCreatedAtDesc(member)
                .stream()
                .map(OrderResponseDto::from)
                .toList();
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

    // 관리자 주문 목록 조회
    public List<OrderResponseDto> getOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(OrderResponseDto::from)
                .toList();
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
    public void payOrder(Order order) {
        order.updateStatus(OrderStatus.PAID);
    }

    // 관리자 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(Long orderId, OrderStatusUpdateRequestDto requestDto) {
        // 주문 조회
        Order order = getOrder(orderId);

        // 주문 상태 변경
        order.updateStatus(requestDto.getStatus());

        return OrderResponseDto.from(order);
    }
}
