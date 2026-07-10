package com.earthy.shop.domain.order.repository;

import com.earthy.shop.domain.member.entity.Member;
import com.earthy.shop.domain.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 회원별 주문 목록 조회
    List<Order> findByMemberOrderByCreatedAtDesc(Member member);

    // 회원 주문 단건 조회
    Optional<Order> findByIdAndMember(Long orderId, Member member);

    // 관리자 주문 목록 조회
    List<Order> findAllByOrderByCreatedAtDesc();

    // 주문번호 존재 여부 조회
    boolean existsByOrderNumber(String orderNumber);
}
