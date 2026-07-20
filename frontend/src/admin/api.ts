const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const ADMIN_ACCESS_TOKEN_KEY = "earthyAdminAccessToken";
const ADMIN_REFRESH_TOKEN_KEY = "earthyAdminRefreshToken";
const ADMIN_AUTH_CLEARED_EVENT = "earthy-admin-auth-cleared";

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface AdminLoginResponse {
  accessToken: string;
  refreshToken: string;
}

export type ProductCategory = "POSTCARD" | "POSTER" | "ETC";
export type AddonType = "FRAME";
export type OrderStatus = "PENDING" | "PAID" | "PREPARING" | "SHIPPED" | "DELIVERED" | "CANCELED";

export interface AdminProduct {
  id: number;
  name: string;
  category: ProductCategory;
  categoryDescription: string;
  price: number;
  imageUrl: string;
  description: string;
  stockQuantity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductSaveRequest {
  name: string;
  category: ProductCategory;
  price: number;
  imageUrl: string;
  description: string;
  stockQuantity: number;
}

export interface AdminAddon {
  id: number;
  name: string;
  type: AddonType;
  typeDescription: string;
  price: number;
  stockQuantity: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AddonSaveRequest {
  name: string;
  type: AddonType;
  price: number;
  stockQuantity: number;
}

export interface OrderItem {
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

export interface AdminOrder {
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
  status: OrderStatus;
  statusDescription: string;
  items: OrderItem[];
  createdAt: string;
  carrier: string | null;
  trackingNumber: string | null;
}

let refreshRequest: Promise<AdminLoginResponse> | null = null;

export function getStoredAdminAccessToken() {
  return localStorage.getItem(ADMIN_ACCESS_TOKEN_KEY);
}

export function saveAdminTokens(tokens: AdminLoginResponse) {
  localStorage.setItem(ADMIN_ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(ADMIN_REFRESH_TOKEN_KEY, tokens.refreshToken);
}

export function clearAdminTokens() {
  localStorage.removeItem(ADMIN_ACCESS_TOKEN_KEY);
  localStorage.removeItem(ADMIN_REFRESH_TOKEN_KEY);
  window.dispatchEvent(new Event(ADMIN_AUTH_CLEARED_EVENT));
}

export function onAdminAuthCleared(handler: () => void) {
  window.addEventListener(ADMIN_AUTH_CLEARED_EVENT, handler);
  return () => window.removeEventListener(ADMIN_AUTH_CLEARED_EVENT, handler);
}

// 관리자 API 요청
async function adminRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetchWithAdminAuth(path, options);

  // 액세스 토큰 만료 시 관리자 리프레시 토큰으로 재발급
  if (isAuthError(response) && shouldTryRefresh(path)) {
    const refreshed = await refreshAdminAccessToken();

    if (refreshed) {
      const retryResponse = await fetchWithAdminAuth(path, options);
      const retryBody = await parseApiResponse<T>(retryResponse);

      if (!retryResponse.ok || !retryBody.success) {
        throw new Error(retryBody.message || "요청 실패");
      }

      return retryBody.data;
    }
  }

  const body = await parseApiResponse<T>(response);

  if (!response.ok || !body.success) {
    throw new Error(body.message || "요청 실패");
  }

  return body.data;
}

async function fetchWithAdminAuth(path: string, options: RequestInit = {}) {
  const headers = new Headers(options.headers);
  const accessToken = localStorage.getItem(ADMIN_ACCESS_TOKEN_KEY);

  if (!headers.has("Content-Type") && options.body) {
    headers.set("Content-Type", "application/json");
  }

  if (accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
  });
}

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const contentType = response.headers.get("Content-Type") ?? "";

  if (!contentType.includes("application/json")) {
    throw new Error("서버 응답 형식이 올바르지 않습니다. 백엔드 실행 상태를 확인해주세요.");
  }

  return (await response.json()) as ApiResponse<T>;
}

function isAuthError(response: Response) {
  return response.status === 401 || response.status === 403;
}

function shouldTryRefresh(path: string) {
  return !path.startsWith("/api/admin/auth/");
}

async function refreshAdminAccessToken() {
  const refreshToken = localStorage.getItem(ADMIN_REFRESH_TOKEN_KEY);

  if (!refreshToken) {
    clearAdminTokens();
    return null;
  }

  try {
    refreshRequest ??= requestAdminRefreshToken(refreshToken);
    const loginResponse = await refreshRequest;
    saveAdminTokens(loginResponse);
    return loginResponse;
  } catch {
    clearAdminTokens();
    return null;
  } finally {
    refreshRequest = null;
  }
}

async function requestAdminRefreshToken(refreshToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/admin/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });
  const body = await parseApiResponse<AdminLoginResponse>(response);

  if (!response.ok || !body.success) {
    throw new Error(body.message || "토큰 재발급 실패");
  }

  return body.data;
}

export async function loginAdmin(email: string, password: string) {
  const response = await fetch(`${API_BASE_URL}/api/admin/auth/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email, password }),
  });
  const body = await parseApiResponse<AdminLoginResponse>(response);

  if (!response.ok || !body.success) {
    throw new Error(body.message || "관리자 로그인 실패");
  }

  saveAdminTokens(body.data);
  return body.data;
}

export async function logoutAdmin() {
  const refreshToken = localStorage.getItem(ADMIN_REFRESH_TOKEN_KEY);

  if (!refreshToken) {
    clearAdminTokens();
    return;
  }

  try {
    await adminRequest<void>("/api/admin/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  } finally {
    clearAdminTokens();
  }
}

export function updateAdminPassword(currentPassword: string, newPassword: string) {
  return adminRequest<void>("/api/admin/password", {
    method: "PATCH",
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

function createPageQuery(page = 0, size = 20) {
  return `page=${page}&size=${size}`;
}

export function getAdminProducts(page = 0, size = 20) {
  return getAdminProductsPage(page, size).then((pageData) => pageData.content);
}

export function getAdminProductsPage(page = 0, size = 20) {
  return adminRequest<PageResponse<AdminProduct>>(`/api/admin/products?${createPageQuery(page, size)}`);
}

export function createAdminProduct(requestBody: ProductSaveRequest) {
  return adminRequest<AdminProduct>("/api/admin/products", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateAdminProduct(productId: number, requestBody: ProductSaveRequest) {
  return adminRequest<AdminProduct>(`/api/admin/products/${productId}`, {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function deactivateAdminProduct(productId: number) {
  return adminRequest<AdminProduct>(`/api/admin/products/${productId}/deactivate`, {
    method: "PATCH",
  });
}

export function activateAdminProduct(productId: number) {
  return adminRequest<AdminProduct>(`/api/admin/products/${productId}/activate`, {
    method: "PATCH",
  });
}

export function deleteAdminProduct(productId: number) {
  return adminRequest<void>(`/api/admin/products/${productId}`, {
    method: "DELETE",
  });
}

export function getAdminAddons(page = 0, size = 20) {
  return getAdminAddonsPage(page, size).then((pageData) => pageData.content);
}

export function getAdminAddonsPage(page = 0, size = 20) {
  return adminRequest<PageResponse<AdminAddon>>(`/api/admin/addons?${createPageQuery(page, size)}`);
}

export function createAdminAddon(requestBody: AddonSaveRequest) {
  return adminRequest<AdminAddon>("/api/admin/addons", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateAdminAddon(addonId: number, requestBody: AddonSaveRequest) {
  return adminRequest<AdminAddon>(`/api/admin/addons/${addonId}`, {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function deactivateAdminAddon(addonId: number) {
  return adminRequest<AdminAddon>(`/api/admin/addons/${addonId}/deactivate`, {
    method: "PATCH",
  });
}

export function activateAdminAddon(addonId: number) {
  return adminRequest<AdminAddon>(`/api/admin/addons/${addonId}/activate`, {
    method: "PATCH",
  });
}

export function deleteAdminAddon(addonId: number) {
  return adminRequest<void>(`/api/admin/addons/${addonId}`, {
    method: "DELETE",
  });
}

export function getAdminOrders(page = 0, size = 20) {
  return getAdminOrdersPage(page, size).then((pageData) => pageData.content);
}

export function getAdminOrdersPage(page = 0, size = 20) {
  return adminRequest<PageResponse<AdminOrder>>(`/api/admin/orders?${createPageQuery(page, size)}`);
}

export function getAdminOrder(orderId: number) {
  return adminRequest<AdminOrder>(`/api/admin/orders/${orderId}`);
}

export function updateAdminOrderStatus(
  orderId: number,
  requestBody: { status: OrderStatus; carrier?: string; trackingNumber?: string }
) {
  return adminRequest<AdminOrder>(`/api/admin/orders/${orderId}/status`, {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function cancelAdminOrder(orderId: number, cancelReason = "") {
  return adminRequest<AdminOrder>(`/api/admin/orders/${orderId}/cancel`, {
    method: "PATCH",
    body: JSON.stringify({ cancelReason }),
  });
}
