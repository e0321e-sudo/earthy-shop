import { request } from "./http";
import type { MemberResponse } from "./auth";

export interface MemberUpdateRequest {
  name: string;
  phone: string;
}

export interface MemberPasswordUpdateRequest {
  currentPassword: string;
  newPassword: string;
}

export function getMyInfo(): Promise<MemberResponse> {
  return request<MemberResponse>("/api/member/me");
}

export function updateMyInfo(requestBody: MemberUpdateRequest): Promise<MemberResponse> {
  return request<MemberResponse>("/api/member/me", {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function updateMyPassword(requestBody: MemberPasswordUpdateRequest): Promise<void> {
  return request<void>("/api/member/me/password", {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}

export function deactivateMyAccount(): Promise<void> {
  return request<void>("/api/member/me", {
    method: "DELETE",
  });
}
