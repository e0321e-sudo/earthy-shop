const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";
const ACCESS_TOKEN_KEY = "earthyAccessToken";
const REFRESH_TOKEN_KEY = "earthyRefreshToken";
const AUTH_CLEARED_EVENT = "earthy-auth-cleared";

interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

let refreshRequest: Promise<LoginResponse> | null = null;

// 공통 API 요청
export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetchWithAuth(path, options);

  // 액세스 토큰 만료 시 토큰 재발급 후 원래 요청 재시도
  if (isAuthError(response) && shouldTryRefresh(path)) {
    const refreshed = await refreshAccessToken();

    if (refreshed) {
      const retryResponse = await fetchWithAuth(path, options);
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

// 액세스 토큰 포함 요청
async function fetchWithAuth(path: string, options: RequestInit = {}) {
  const accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
  const headers = new Headers(options.headers);

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

// 공통 응답 파싱
async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const contentType = response.headers.get("Content-Type") ?? "";

  if (!contentType.includes("application/json")) {
    throw new Error("서버 응답 형식이 올바르지 않습니다. 백엔드 실행 상태를 확인해주세요.");
  }

  return (await response.json()) as ApiResponse<T>;
}

// 인증 실패 여부
function isAuthError(response: Response) {
  return response.status === 401 || response.status === 403;
}

// 인증 API는 토큰 재발급 재시도 제외
function shouldTryRefresh(path: string) {
  return !path.startsWith("/api/member/auth/");
}

// 리프레시 토큰으로 토큰 재발급
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);

  if (!refreshToken) {
    clearStoredTokens();
    return null;
  }

  try {
    refreshRequest ??= requestRefreshToken(refreshToken);
    const loginResponse = await refreshRequest;

    localStorage.setItem(ACCESS_TOKEN_KEY, loginResponse.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, loginResponse.refreshToken);

    return loginResponse;
  } catch {
    clearStoredTokens();
    return null;
  } finally {
    refreshRequest = null;
  }
}

// 토큰 재발급 API 직접 호출
async function requestRefreshToken(refreshToken: string) {
  const response = await fetch(`${API_BASE_URL}/api/member/auth/refresh`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      refreshToken,
    }),
  });
  const body = await parseApiResponse<LoginResponse>(response);

  if (!response.ok || !body.success) {
    throw new Error(body.message || "토큰 재발급 실패");
  }

  return body.data;
}

// 인증 상태 초기화
function clearStoredTokens() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.dispatchEvent(new Event(AUTH_CLEARED_EVENT));
}
