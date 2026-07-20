import { request } from "./http";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface OrderItemResponse {
  orderItemId: number;
  productId: number;
  productName: string;
  productImageUrl: string;
  productPrice: number;
  addonId: number | null;
  addonName: string | null;
  addonPrice: number;
  addonQuantity: number;
  quantity: number;
  itemTotalPrice: number;
}

export interface OrderResponse {
  orderId: number;
  orderNumber: string;
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  address: string;
  detailAddress: string;
  deliveryMemo: string | null;
  productTotalPrice: number;
  deliveryFee: number;
  remoteAreaDeliveryFee: number;
  totalPrice: number;
  paymentMethod: string | null;
  status: string;
  statusDescription: string;
  items: OrderItemResponse[];
  createdAt: string;
  carrier?: string | null;
  trackingNumber?: string | null;
}

export interface OrderCreateRequest {
  cartItemIds?: number[];
  receiverName: string;
  receiverPhone: string;
  zipCode: string;
  address: string;
  detailAddress: string;
  deliveryMemo: string;
}

export function createOrder(requestBody: OrderCreateRequest): Promise<OrderResponse> {
  return request<OrderResponse>("/api/orders", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function getMyOrders(): Promise<OrderResponse[]> {
  return getMyOrdersPage().then((page) => page.content);
}

export function getMyOrdersPage(page = 0, size = 20): Promise<PageResponse<OrderResponse>> {
  return request<PageResponse<OrderResponse>>(`/api/orders?page=${page}&size=${size}`);
}

export function getMyOrder(orderId: number): Promise<OrderResponse> {
  return request<OrderResponse>(`/api/orders/${orderId}`);
}

export function cancelMyOrder(orderId: number, cancelReason = ""): Promise<OrderResponse> {
  return request<OrderResponse>(`/api/orders/${orderId}/cancel`, {
    method: "PATCH",
    body: JSON.stringify({ cancelReason }),
  });
}
