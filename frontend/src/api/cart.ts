import { request } from "./http";

export interface CartItemResponse {
  cartItemId: number;
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

export interface CartResponse {
  items: CartItemResponse[];
  totalPrice: number;
}

export interface CartItemAddRequest {
  productId: number;
  addonId: number | null;
  addonQuantity: number | null;
  quantity: number;
}

export interface CartItemQuantityUpdateRequest {
  quantity: number;
  addonQuantity: number | null;
}

export function getCart(): Promise<CartResponse> {
  return request<CartResponse>("/api/cart");
}

export function addCartItem(requestBody: CartItemAddRequest): Promise<CartResponse> {
  return request<CartResponse>("/api/cart", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateCartItemQuantity(
  cartItemId: number,
  requestBody: CartItemQuantityUpdateRequest
): Promise<CartResponse> {
  return request<CartResponse>(`/api/cart/${cartItemId}`, {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function deleteCartItem(cartItemId: number): Promise<CartResponse> {
  return request<CartResponse>(`/api/cart/${cartItemId}`, {
    method: "DELETE",
  });
}

export function clearCart(): Promise<void> {
  return request<void>("/api/cart", {
    method: "DELETE",
  });
}
