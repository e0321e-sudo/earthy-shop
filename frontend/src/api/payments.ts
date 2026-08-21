import { request } from "./http";

export interface PaymentConfirmRequest {
  orderId: number;
  paymentKey: string;
  amount: number;
  method?: string;
}

export interface PaymentResponse {
  paymentId: number;
  orderId: number;
  paymentKey: string;
  orderNumber: string;
  amount: number;
  method: string;
  status: string;
  statusDescription: string;
  createdAt: string;
}

export function confirmPayment(requestBody: PaymentConfirmRequest, idempotencyKey: string): Promise<PaymentResponse> {
  return request<PaymentResponse>("/api/payments/confirm", {
    method: "POST",
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
    body: JSON.stringify(requestBody),
  });
}
