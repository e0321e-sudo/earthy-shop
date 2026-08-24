package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.common.response.PageResponseDto;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.cart.dto.response.CartItemResponseDto;
import com.earthy.shop.domain.cart.dto.response.CartResponseDto;
import com.earthy.shop.domain.cart.service.CartService;
import com.earthy.shop.domain.notification.event.ShippingStartedNotificationEvent;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.member.service.MemberService;
import com.earthy.shop.domain.order.dto.request.OrderCreateRequestDto;
import com.earthy.shop.domain.order.dto.request.OrderStatusUpdateRequestDto;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.repository.OrderRepository;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.product.service.ProductService;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private CartService cartService;

    @Mock
    private DeliveryFeeCalculator deliveryFeeCalculator;

    @Mock
    private ProductService productService;

    @Mock
    private AddonService addonService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void 선택한_장바구니_상품으로_주문을_생성한다() {
        // given
        Member member = member();
        CartItemResponseDto selectedItem = cartItem(1L, 1L, null, 3500, 0, 0, 2);
        CartItemResponseDto notSelectedItem = cartItem(2L, 2L, null, 3500, 0, 0, 1);
        CartResponseDto cart = CartResponseDto.from(List.of(selectedItem, notSelectedItem));
        OrderCreateRequestDto requestDto = orderRequest(List.of(1L));

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com")).willReturn(cart);
        given(deliveryFeeCalculator.calculateBaseDeliveryFee(7000)).willReturn(2500);
        given(deliveryFeeCalculator.calculateRemoteAreaDeliveryFee("51100", "경남 창원시 소답동"))
                .willReturn(0);
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> {
                    Order order = invocation.getArgument(0);
                    TestEntityUtils.setId(order, 1L);
                    TestEntityUtils.setField(order, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 0));
                    return order;
                });

        // when
        OrderResponseDto response = orderService.createOrder("test@example.com", requestDto, "test-idempotency-key");

        // then
        verify(memberService).registerOrderContactIfBlank(
                member,
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호"
        );
        verify(productService).validateStock(1L, 2);
        verify(idempotencyService).complete(any(), any(), any());
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.productTotalPrice()).isEqualTo(7000);
        assertThat(response.deliveryFee()).isEqualTo(2500);
        assertThat(response.totalPrice()).isEqualTo(9500);
    }

    @Test
    void 멱등성_키가_없으면_주문_생성_예외가_발생한다() {
        // given
        OrderCreateRequestDto requestDto = orderRequest(null);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.createOrder("test@example.com", requestDto, ""))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED)
                );
        verify(idempotencyService, never()).find(any(), any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 완료된_멱등성_요청이면_기존_주문_결과를_반환한다() {
        // given
        Member member = member();
        Order order = savedOrder(order(member, 3500, 2500, 0));

        IdempotencyKey completedKey = new IdempotencyKey(
                "test@example.com",
                "test-idempotency-key",
                "/api/orders"
        );
        completedKey.complete(1L, "주문 생성 성공");

        given(idempotencyService.find("test@example.com", "test-idempotency-key", "/api/orders"))
                .willReturn(completedKey);
        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(orderRepository.findByIdAndMember(1L, member))
                .willReturn(Optional.of(order));

        // when
        OrderResponseDto response = orderService.createOrder(
                "test@example.com",
                orderRequest(null),
                "test-idempotency-key"
        );

        // then
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        verify(idempotencyService, never()).create(any(), any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 처리중인_멱등성_요청이면_중복_요청_예외가_발생한다() {
        // given
        IdempotencyKey processingKey = new IdempotencyKey(
                "test@example.com",
                "test-idempotency-key",
                "/api/orders"
        );

        given(idempotencyService.find("test@example.com", "test-idempotency-key", "/api/orders"))
                .willReturn(processingKey);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.createOrder(
                        "test@example.com",
                        orderRequest(null),
                        "test-idempotency-key"
                ))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING)
                );
        verify(idempotencyService, never()).create(any(), any(), any());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 전체_장바구니_상품으로_주문을_생성한다() {
        // given
        Member member = member();
        CartItemResponseDto firstItem = cartItem(1L, 1L, null, 3500, 0, 0, 2);
        CartItemResponseDto secondItem = cartItem(2L, 2L, null, 12000, 0, 0, 1);
        CartResponseDto cart = CartResponseDto.from(List.of(firstItem, secondItem));
        OrderCreateRequestDto requestDto = orderRequest(null);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com")).willReturn(cart);
        given(deliveryFeeCalculator.calculateBaseDeliveryFee(19000)).willReturn(2500);
        given(deliveryFeeCalculator.calculateRemoteAreaDeliveryFee("51100", "경남 창원시 소답동"))
                .willReturn(0);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> savedOrder(invocation.getArgument(0)));

        // when
        OrderResponseDto response = orderService.createOrder("test@example.com", requestDto, "test-idempotency-key");

        // then
        verify(productService).validateStock(1L, 2);
        verify(productService).validateStock(2L, 1);
        assertThat(response.items()).hasSize(2);
        assertThat(response.productTotalPrice()).isEqualTo(19000);
        assertThat(response.totalPrice()).isEqualTo(21500);
    }

    @Test
    void 삼만원_이상_주문은_무료배송으로_생성한다() {
        // given
        Member member = member();
        CartItemResponseDto cartItem = cartItem(1L, 1L, null, 35000, 0, 0, 1);
        CartResponseDto cart = CartResponseDto.from(List.of(cartItem));
        OrderCreateRequestDto requestDto = orderRequest(null);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com")).willReturn(cart);
        given(deliveryFeeCalculator.calculateBaseDeliveryFee(35000)).willReturn(0);
        given(deliveryFeeCalculator.calculateRemoteAreaDeliveryFee("51100", "경남 창원시 소답동"))
                .willReturn(0);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> savedOrder(invocation.getArgument(0)));

        // when
        OrderResponseDto response = orderService.createOrder("test@example.com", requestDto, "test-idempotency-key");

        // then
        assertThat(response.deliveryFee()).isZero();
        assertThat(response.totalPrice()).isEqualTo(35000);
    }

    @Test
    void 도서산간_지역은_지역별_배송비를_추가한다() {
        // given
        Member member = member();
        CartItemResponseDto cartItem = cartItem(1L, 1L, null, 3500, 0, 0, 1);
        CartResponseDto cart = CartResponseDto.from(List.of(cartItem));
        OrderCreateRequestDto requestDto = new OrderCreateRequestDto(
                null,
                "홍길동",
                "010-1234-5678",
                "63000",
                "제주특별자치도 제주시",
                "101호",
                "문 앞에 놓아주세요"
        );

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com")).willReturn(cart);
        given(deliveryFeeCalculator.calculateBaseDeliveryFee(3500)).willReturn(2500);
        given(deliveryFeeCalculator.calculateRemoteAreaDeliveryFee("63000", "제주특별자치도 제주시"))
                .willReturn(2000);
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> savedOrder(invocation.getArgument(0)));

        // when
        OrderResponseDto response = orderService.createOrder("test@example.com", requestDto, "test-idempotency-key");

        // then
        assertThat(response.deliveryFee()).isEqualTo(2500);
        assertThat(response.remoteAreaDeliveryFee()).isEqualTo(2000);
        assertThat(response.totalPrice()).isEqualTo(8000);
    }

    @Test
    void 장바구니가_비어있으면_주문_생성_예외가_발생한다() {
        // given
        Member member = member();
        OrderCreateRequestDto requestDto = orderRequest(null);

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com")).willReturn(CartResponseDto.from(List.of()));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.createOrder("test@example.com", requestDto, "test-idempotency-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.EMPTY_CART)
                );
    }

    @Test
    void 선택한_장바구니_항목이_없으면_예외가_발생한다() {
        // given
        Member member = member();
        CartItemResponseDto cartItem = cartItem(1L, 1L, null, 3500, 0, 0, 1);
        OrderCreateRequestDto requestDto = orderRequest(List.of(1L, 999L));

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(cartService.getCart("test@example.com"))
                .willReturn(CartResponseDto.from(List.of(cartItem)));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.createOrder("test@example.com", requestDto, "test-idempotency-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CART_ITEM_NOT_FOUND)
                );
    }

    @Test
    void 내_주문_목록은_주문대기_상태를_제외하고_조회한다() {
        // given
        Pageable pageable = PageRequest.of(0, 20);
        Member member = member();
        Order order = savedOrder(order(member, 3500, 2500, 0));
        order.pay("카드");

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(orderRepository.findByMemberAndStatusNot(member, OrderStatus.PENDING, pageable))
                .willReturn(new PageImpl<>(List.of(order), pageable, 1));

        // when
        PageResponseDto<OrderResponseDto> response = orderService.getMyOrders("test@example.com", pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void 내_주문_상세는_주문대기_상태를_제외하고_조회한다() {
        // given
        Member member = member();
        Order order = savedOrder(order(member, 3500, 2500, 0));
        order.pay("카드");

        given(memberService.getActiveMember("test@example.com")).willReturn(member);
        given(orderRepository.findByIdAndMemberAndStatusNot(1L, member, OrderStatus.PENDING))
                .willReturn(Optional.of(order));

        // when
        OrderResponseDto response = orderService.getMyOrder("test@example.com", 1L);

        // then
        assertThat(response.orderId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void 관리자가_결제완료_주문을_상품준비중으로_변경한다() {
        // given
        Order order = savedOrder(order(member(), 3500, 2500, 0));
        order.pay("카드");
        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto(
                OrderStatus.PREPARING,
                null,
                null
        );

        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

        // when
        OrderResponseDto response = orderService.updateOrderStatus(1L, requestDto);

        // then
        verify(orderRepository).findByIdForUpdate(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.PREPARING);
    }

    @Test
    void 배송중으로_변경할_때_배송정보가_없으면_예외가_발생한다() {
        // given
        Order order = savedOrder(order(member(), 3500, 2500, 0));
        order.pay("카드");
        order.updateStatus(OrderStatus.PREPARING);
        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto(
                OrderStatus.SHIPPED,
                "",
                ""
        );

        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.updateOrderStatus(1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SHIPPING_INFO_REQUIRED)
                );
    }

    @Test
    void 배송중으로_변경하면_배송정보를_저장하고_알림_이벤트를_발행한다() {
        // given
        Order order = savedOrder(order(member(), 3500, 2500, 0));
        order.pay("카드");
        order.updateStatus(OrderStatus.PREPARING);
        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto(
                OrderStatus.SHIPPED,
                "우체국",
                "1234567890"
        );

        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

        // when
        OrderResponseDto response = orderService.updateOrderStatus(1L, requestDto);

        // then
        verify(eventPublisher).publishEvent(any(ShippingStartedNotificationEvent.class));
        assertThat(response.status()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(response.carrier()).isEqualTo("우체국");
        assertThat(response.trackingNumber()).isEqualTo("1234567890");
    }

    @Test
    void 잘못된_주문_상태_변경은_예외가_발생한다() {
        // given
        Order order = savedOrder(order(member(), 3500, 2500, 0));
        OrderStatusUpdateRequestDto requestDto = new OrderStatusUpdateRequestDto(
                OrderStatus.SHIPPED,
                "우체국",
                "1234567890"
        );

        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderService.updateOrderStatus(1L, requestDto))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATUS_CHANGE)
                );
    }

    private Member member() {
        Member member = new Member("test@example.com", "encodedPassword", "홍길동", "010-1234-5678");
        TestEntityUtils.setId(member, 1L);
        return member;
    }

    private CartItemResponseDto cartItem(
            Long cartItemId,
            Long productId,
            Long addonId,
            int productPrice,
            int addonPrice,
            int addonQuantity,
            int quantity
    ) {
        int totalPrice = (productPrice * quantity) + (addonPrice * addonQuantity);

        return new CartItemResponseDto(
                cartItemId,
                productId,
                "sunset sea postcard",
                "/assets/products/sunset-sea.jpeg",
                productPrice,
                null,
                null,
                0,
                productPrice,
                addonId,
                addonId == null ? null : "A3 원목 액자",
                addonPrice,
                addonQuantity,
                List.of(),
                quantity,
                totalPrice
        );
    }

    private OrderCreateRequestDto orderRequest(List<Long> cartItemIds) {
        return new OrderCreateRequestDto(
                cartItemIds,
                "홍길동",
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호",
                "문 앞에 놓아주세요"
        );
    }

    private Order order(Member member, int productTotalPrice, int deliveryFee, int remoteAreaDeliveryFee) {
        Order order = new Order(
                "ORD-20260728-ABC12345",
                member,
                "홍길동",
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호",
                "문 앞에 놓아주세요",
                productTotalPrice,
                deliveryFee,
                remoteAreaDeliveryFee
        );
        order.addOrderItem(orderItem());
        return order;
    }

    private Order savedOrder(Order order) {
        TestEntityUtils.setId(order, 1L);
        TestEntityUtils.setField(order, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 0));
        return order;
    }

    private OrderItem orderItem() {
        return new OrderItem(
                1L,
                "sunset sea postcard",
                "/assets/products/sunset-sea.jpeg",
                3500,
                null,
                null,
                0,
                0,
                1
        );
    }
}
