package com.earthy.shop.domain.payment.dto.portone;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponseDto(
        String id,
        String paymentId,
        String transactionId,
        String status,
        String orderName,
        String payMethod,
        Amount amount,
        Method method,
        NestedPayment payment,
        Cancellation cancellation
) {

    public String resolvedPaymentId(String fallbackPaymentId) {
        if (id != null && !id.isBlank()) {
            return id;
        }

        if (paymentId != null && !paymentId.isBlank()) {
            return paymentId;
        }

        return fallbackPaymentId;
    }

    public int totalAmount() {
        return amount == null || amount.total() == null ? 0 : amount.total();
    }

    public String resolvedMethod() {
        if (method != null && method.type() != null && !method.type().isBlank()) {
            return translateMethod(method.type());
        }

        if (payMethod != null && !payMethod.isBlank()) {
            return translateMethod(payMethod);
        }

        return "결제";
    }

    public boolean isCanceledPayment() {
        return isCanceledPaymentStatus(status);
    }

    public boolean isCancelSucceeded() {
        return isCanceledPayment()
                || isSucceededCancellationStatus(status)
                || isCanceledPaymentStatus(nestedPaymentStatus())
                || isSucceededCancellationStatus(cancellationStatus());
    }

    public String nestedPaymentStatus() {
        return payment == null ? null : payment.status();
    }

    public String cancellationStatus() {
        return cancellation == null ? null : cancellation.status();
    }

    private String translateMethod(String value) {
        return switch (value) {
            case "CARD" -> "카드";
            case "MOBILE", "MOBILE_PHONE" -> "휴대폰 결제";
            default -> value;
        };
    }

    private boolean isCanceledPaymentStatus(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toUpperCase();
        return "CANCELED".equals(normalized) || "CANCELLED".equals(normalized);
    }

    private boolean isSucceededCancellationStatus(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized = value.trim().toUpperCase();
        return "SUCCEEDED".equals(normalized)
                || "SUCCESS".equals(normalized)
                || "SUCCESSFUL".equals(normalized)
                || "COMPLETED".equals(normalized);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(Integer total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Method(String type, String provider) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NestedPayment(String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cancellation(String status) {
    }
}
