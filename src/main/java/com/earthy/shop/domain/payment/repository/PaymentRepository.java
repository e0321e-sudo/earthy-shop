package com.earthy.shop.domain.payment.repository;

import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.payment.entity.Payment;
import com.earthy.shop.domain.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // 주문 결제 이력 조회
    List<Payment> findByOrderOrderByCreatedAtDesc(Order order);

    // 주문 완료 결제 조회
    Optional<Payment> findByOrderAndStatus(Order order, PaymentStatus status);

    // 결제 키 존재 여부 조회
    boolean existsByPaymentKey(String paymentKey);
}
