import { request } from "./http";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "";

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
  termsAgreed: boolean;
  privacyAgreed: boolean;
  marketingAgreed: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface EmailFindRequest {
  name: string;
  phone: string;
}

export interface PasswordFindRequest {
  email: string;
}

export interface MemberResponse {
  id: number;
  email: string;
  name: string;
  phone: string;
  zipCode: string | null;
  address: string | null;
  detailAddress: string | null;
  provider: "LOCAL" | "KAKAO" | "NAVER";
  role: "MEMBER" | "ADMIN";
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
}

export interface EmailFindResponse {
  email: string;
  provider: "LOCAL" | "KAKAO" | "NAVER";
  providerDescription: string;
}

export function signup(requestBody: SignupRequest): Promise<MemberResponse> {
  return request<MemberResponse>("/api/member/auth/signup", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function login(requestBody: LoginRequest): Promise<LoginResponse> {
  return request<LoginResponse>("/api/member/auth/login", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function logout(refreshTokenValue: string): Promise<void> {
  return request<void>("/api/member/auth/logout", {
    method: "POST",
    body: JSON.stringify({
      refreshToken: refreshTokenValue,
    }),
  });
}

export function refreshToken(refreshTokenValue: string): Promise<LoginResponse> {
  return request<LoginResponse>("/api/member/auth/refresh", {
    method: "POST",
    body: JSON.stringify({
      refreshToken: refreshTokenValue,
    }),
  });
}

export function findEmail(requestBody: EmailFindRequest): Promise<EmailFindResponse> {
  return request<EmailFindResponse>("/api/member/auth/find-email", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function findPassword(requestBody: PasswordFindRequest): Promise<void> {
  return request<void>("/api/member/auth/find-password", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function getKakaoLoginUrl() {
  return `${API_BASE_URL}/api/oauth/kakao`;
}
