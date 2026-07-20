package com.earthy.shop.domain.order.service;

import org.springframework.stereotype.Component;

@Component
public class DeliveryFeeCalculator {

    private static final int FREE_DELIVERY_MIN_AMOUNT = 30000;
    private static final int BASE_DELIVERY_FEE = 2500;
    private static final int REMOTE_AREA_DELIVERY_FEE = 500;

    // 기본 배송비 계산
    public int calculateBaseDeliveryFee(int productTotalPrice) {
        if (productTotalPrice >= FREE_DELIVERY_MIN_AMOUNT) {
            return 0;
        }

        return BASE_DELIVERY_FEE;
    }

    // 지역별 배송비 계산
    public int calculateRemoteAreaDeliveryFee(String zipCode, String address) {
        if (isRemoteArea(zipCode, address)) {
            return REMOTE_AREA_DELIVERY_FEE;
        }

        return 0;
    }

    // 도서산간 지역 여부 확인
    private boolean isRemoteArea(String zipCode, String address) {
        return isJeju(zipCode, address)
                || isIslandArea(address)
                || isMountainArea(address);
    }

    // 제주 지역 여부 확인
    private boolean isJeju(String zipCode, String address) {
        return startsWithAny(zipCode, "63")
                || containsAny(address, "제주특별자치도", "제주시", "서귀포시");
    }

    // 도서 지역 여부 확인
    private boolean isIslandArea(String address) {
        return containsAny(
                address,
                "울릉군",
                "백령면",
                "대청면",
                "소청",
                "연평면",
                "흑산면",
                "홍도",
                "비금면",
                "도초면",
                "신의면",
                "하의면",
                "장산면",
                "안좌면",
                "팔금면",
                "암태면",
                "자은면",
                "압해읍",
                "완도군",
                "청산면",
                "노화읍",
                "보길면",
                "금일읍",
                "금당면",
                "생일면",
                "소안면",
                "진도군",
                "조도면",
                "남해군",
                "욕지면",
                "한산면",
                "사량면"
        );
    }

    // 산간 지역 여부 확인
    private boolean isMountainArea(String address) {
        return containsAny(
                address,
                "강원특별자치도 인제군",
                "강원특별자치도 양양군",
                "강원특별자치도 평창군",
                "강원특별자치도 정선군",
                "강원특별자치도 화천군",
                "강원특별자치도 양구군"
        );
    }

    // 우편번호 앞자리 확인
    private boolean startsWithAny(String value, String... prefixes) {
        if (value == null) {
            return false;
        }

        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    // 주소 키워드 포함 여부 확인
    private boolean containsAny(String value, String... keywords) {
        if (value == null) {
            return false;
        }

        for (String keyword : keywords) {
            if (value.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}
