import { request } from "./http";

export interface SignupRequest {
  email: string;
  password: string;
  name: string;
  phone: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface MemberResponse {
  id: number;
  email: string;
  name: string;
  phone: string;
  role: "MEMBER" | "ADMIN";
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
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
