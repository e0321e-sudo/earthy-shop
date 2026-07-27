package com.earthy.shop.domain.order.repository;

import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.order.entity.Order;
import com.earthy.shop.domain.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원별 주문 목록 조회
    Page<Order> findByMember(Member member, Pageable pageable);

    // 회원별 결제 전 주문 제외 목록 조회
    Page<Order> findByMemberAndStatusNot(Member member, OrderStatus status, Pageable pageable);

    // 회원 주문 단건 조회
    Optional<Order> findByIdAndMember(Long orderId, Member member);

    // 회원 결제 전 주문 제외 단건 조회
    Optional<Order> findByIdAndMemberAndStatusNot(Long orderId, Member member, OrderStatus status);

    // 관리자 주문 상태 제외 목록 조회
    Page<Order> findByStatusNot(OrderStatus status, Pageable pageable);

    // 주문 상태별 개수 조회
    long countByStatus(OrderStatus status);

    // 주문번호 존재 여부 조회
    boolean existsByOrderNumber(String orderNumber);
}
