import { request } from "./http";
import type { PageResponse } from "./products";

export interface NoticeResponse {
  id: number;
  title: string;
  content: string;
  visible: boolean;
  visibleDescription: string;
  createdAt: string;
  updatedAt: string;
}

function createNoticeQuery(keyword: string, page: number, size: number) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (keyword.trim()) {
    params.set("keyword", keyword.trim());
  }

  return params.toString();
}

export function getNoticesPage(keyword = "", page = 0, size = 20): Promise<PageResponse<NoticeResponse>> {
  return request<PageResponse<NoticeResponse>>(`/api/notices?${createNoticeQuery(keyword, page, size)}`);
}

export function getNotice(noticeId: number): Promise<NoticeResponse> {
  return request<NoticeResponse>(`/api/notices/${noticeId}`);
}
