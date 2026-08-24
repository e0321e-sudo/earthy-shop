import { request } from "./http";

export interface CartItemAddonResponse {
  cartItemAddonId: number;
  addonId: number;
  addonName: string;
  addonPrice: number;
  quantity: number;
  totalPrice: number;
}

export interface CartItemResponse {
  cartItemId: number;
  productId: number;
  productName: string;
  productImageUrl: string;
  productPrice: number;
  sizeOptionId: number | null;
  sizeName: string | null;
  sizeAdditionalPrice: number;
  productUnitPrice: number;
  addonId: number | null;
  addonName: string | null;
  addonPrice: number;
  addonQuantity: number;
  addons: CartItemAddonResponse[];
  quantity: number;
  itemTotalPrice: number;
}

export interface CartResponse {
  items: CartItemResponse[];
  totalPrice: number;
}

export interface CartItemAddRequest {
  productId: number;
  productSizeOptionId: number | null;
  addonId: number | null;
  addonQuantity: number | null;
  addons: Array<{
    addonId: number;
    quantity: number;
  }>;
  quantity: number;
}

export interface CartItemQuantityUpdateRequest {
  quantity: number;
  addonQuantity: number | null;
}

export interface CartItemAddonQuantityUpdateRequest {
  quantity: number;
}

export function getCart(): Promise<CartResponse> {
  return request<CartResponse>("/api/cart");
}

export function addCartItem(requestBody: CartItemAddRequest, idempotencyKey: string): Promise<CartResponse> {
  return request<CartResponse>("/api/cart", {
    method: "POST",
    headers: {
      "Idempotency-Key": idempotencyKey,
    },
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

export function updateCartItemAddonQuantity(
  cartItemId: number,
  cartItemAddonId: number,
  requestBody: CartItemAddonQuantityUpdateRequest
): Promise<CartResponse> {
  return request<CartResponse>(`/api/cart/${cartItemId}/addons/${cartItemAddonId}`, {
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
