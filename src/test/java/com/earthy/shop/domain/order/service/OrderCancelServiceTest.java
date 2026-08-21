package com.earthy.shop.domain.order.service;

import com.earthy.shop.common.exception.BusinessException;
import com.earthy.shop.common.exception.ErrorCode;
import com.earthy.shop.common.idempotency.entity.IdempotencyKey;
import com.earthy.shop.common.idempotency.service.IdempotencyService;
import com.earthy.shop.domain.addon.service.AddonService;
import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.order.dto.response.OrderResponseDto;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.entity.OrderItem;
import com.earthy.shop.domain.order.enums.OrderStatus;
import com.earthy.shop.domain.payment.service.PaymentService;
import com.earthy.shop.domain.product.service.ProductService;
import com.earthy.shop.support.TestEntityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderCancelServiceTest {

    @Mock
    private OrderService orderService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private ProductService productService;

    @Mock
    private AddonService addonService;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private OrderCancelService orderCancelService;

    @Test
    void 결제완료_주문을_취소하면_결제취소와_재고복구를_처리한다() {
        // given
        Order order = order();
        order.pay("카드");

        given(orderService.findMyOrderForUpdate("test@example.com", 1L)).willReturn(order);

        // when
        OrderResponseDto response = orderCancelService.cancelMyOrder("test@example.com", 1L, "", "cancel-key");

        // then
        verify(orderService).findMyOrderForUpdate("test@example.com", 1L);
        verify(paymentService).cancelPayment(order, "고객 요청으로 인한 주문 취소");
        verify(productService).increaseStock(1L, 1);
        verify(addonService).increaseStock(1L, 1);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 완료된_주문취소_요청이면_기존_주문결과를_반환한다() {
        // given
        Order order = order();
        order.cancel();
        IdempotencyKey completedKey = new IdempotencyKey(
                "test@example.com",
                "cancel-key",
                "/api/orders/1/cancel"
        );
        completedKey.complete(1L, "주문 취소 성공");

        given(idempotencyService.find("test@example.com", "cancel-key", "/api/orders/1/cancel"))
                .willReturn(completedKey);
        given(orderService.getMyOrder("test@example.com", 1L)).willReturn(OrderResponseDto.from(order));

        // when
        OrderResponseDto response = orderCancelService.cancelMyOrder(
                "test@example.com",
                1L,
                "단순 변심",
                "cancel-key"
        );

        // then
        verify(orderService, never()).findMyOrderForUpdate("test@example.com", 1L);
        verify(paymentService, never()).cancelPayment(order, "단순 변심");
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 처리중인_주문취소_요청이면_중복_요청_예외가_발생한다() {
        // given
        IdempotencyKey processingKey = new IdempotencyKey(
                "test@example.com",
                "cancel-key",
                "/api/orders/1/cancel"
        );

        given(idempotencyService.find("test@example.com", "cancel-key", "/api/orders/1/cancel"))
                .willReturn(processingKey);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderCancelService.cancelMyOrder(
                        "test@example.com",
                        1L,
                        "단순 변심",
                        "cancel-key"
                ))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_REQUEST_PROCESSING)
                );
        verify(orderService, never()).findMyOrderForUpdate("test@example.com", 1L);
    }

    @Test
    void 주문취소_멱등성_키가_없으면_예외가_발생한다() {
        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderCancelService.cancelMyOrder("test@example.com", 1L, "단순 변심", ""))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED)
                );
    }

    @Test
    void 주문대기_주문은_결제취소와_재고복구_없이_취소한다() {
        // given
        Order order = order();

        given(orderService.findMyOrderForUpdate("test@example.com", 1L)).willReturn(order);

        // when
        OrderResponseDto response = orderCancelService.cancelMyOrder("test@example.com", 1L, "단순 변심", "cancel-key");

        // then
        verify(paymentService, never()).cancelPayment(order, "단순 변심");
        verify(productService, never()).increaseStock(1L, 1);
        verify(addonService, never()).increaseStock(1L, 1);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 상품준비중_주문을_취소하면_결제취소와_재고복구를_처리한다() {
        // given
        Order order = order();
        order.pay("카드");
        order.updateStatus(OrderStatus.PREPARING);

        given(orderService.findMyOrderForUpdate("test@example.com", 1L)).willReturn(order);

        // when
        OrderResponseDto response = orderCancelService.cancelMyOrder("test@example.com", 1L, "배송 전 취소", "cancel-key");

        // then
        verify(paymentService).cancelPayment(order, "배송 전 취소");
        verify(productService).increaseStock(1L, 1);
        verify(addonService).increaseStock(1L, 1);
        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
    }

    @Test
    void 관리자가_취소사유_없이_취소하면_관리자_기본사유를_사용한다() {
        // given
        Order order = order();
        order.pay("카드");

        given(orderService.getOrderForUpdate(1L)).willReturn(order);

        // when
        orderCancelService.cancelAdminOrder(1L, null, "admin-cancel-key");

        // then
        verify(orderService).getOrderForUpdate(1L);
        verify(paymentService).cancelPayment(order, "관리자 요청으로 인한 주문 취소");
    }

    @Test
    void 추가상품이_없는_주문_취소_시_추가상품_재고는_복구하지_않는다() {
        // given
        Order order = orderWithoutAddon();
        order.pay("카드");

        given(orderService.findMyOrderForUpdate("test@example.com", 1L)).willReturn(order);

        // when
        orderCancelService.cancelMyOrder("test@example.com", 1L, "고객 요청", "cancel-key");

        // then
        verify(productService).increaseStock(1L, 1);
        verify(addonService, never()).increaseStock(1L, 1);
    }

    @Test
    void 배송중_주문은_취소할_수_없다() {
        // given
        Order order = order();
        TestEntityUtils.setField(order, "status", OrderStatus.SHIPPED);

        given(orderService.findMyOrderForUpdate("test@example.com", 1L)).willReturn(order);

        // when & then
        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> orderCancelService.cancelMyOrder("test@example.com", 1L, "단순 변심", "cancel-key"))
                .satisfies(exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ORDER_NOT_CANCELABLE)
                );
        verify(paymentService, never()).cancelPayment(order, "단순 변심");
    }

    private Order order() {
        Member member = new Member("test@example.com", "encodedPassword", "홍길동", "010-1234-5678");
        TestEntityUtils.setId(member, 1L);

        Order order = new Order(
                "ORD-20260728-ABC12345",
                member,
                "홍길동",
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호",
                "문 앞에 놓아주세요",
                13000,
                2500,
                0
        );
        TestEntityUtils.setId(order, 1L);
        TestEntityUtils.setField(order, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 0));
        order.addOrderItem(new OrderItem(
                1L,
                "sunset sea poster",
                "/assets/products/sunset-sea.jpeg",
                3500,
                1L,
                "A3 원목 액자",
                9500,
                1,
                1
        ));

        return order;
    }

    private Order orderWithoutAddon() {
        Member member = new Member("test@example.com", "encodedPassword", "홍길동", "010-1234-5678");
        TestEntityUtils.setId(member, 1L);

        Order order = new Order(
                "ORD-20260728-ABC12345",
                member,
                "홍길동",
                "010-1234-5678",
                "51100",
                "경남 창원시 소답동",
                "711호",
                "문 앞에 놓아주세요",
                3500,
                2500,
                0
        );
        TestEntityUtils.setId(order, 1L);
        TestEntityUtils.setField(order, "createdAt", LocalDateTime.of(2026, 7, 28, 1, 0));
        order.addOrderItem(new OrderItem(
                1L,
                "sunset sea postcard",
                "/assets/products/sunset-sea.jpeg",
                3500,
                null,
                null,
                0,
                0,
                1
        ));

        return order;
    }
}
