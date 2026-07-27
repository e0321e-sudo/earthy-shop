import { request } from "./http";
import type { PageResponse } from "./products";

export type BoardType = "PRODUCT" | "DELIVERY" | "EXCHANGE_RETURN" | "CANCEL_CHANGE" | "PAYMENT" | "ETC";
export type BoardVisibility = "PUBLIC" | "PRIVATE";

export interface BoardListResponse {
  id: number;
  type: BoardType;
  typeDescription: string;
  title: string;
  writerName: string;
  visibility: BoardVisibility;
  visibilityDescription: string;
  answeredAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BoardResponse extends BoardListResponse {
  content: string;
  answer: string | null;
  mine: boolean;
}

export interface BoardSaveRequest {
  type: BoardType;
  title: string;
  content: string;
  visibility: BoardVisibility;
  postPassword?: string;
}

function createBoardQuery(keyword: string, page: number, size: number) {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });

  if (keyword.trim()) {
    params.set("keyword", keyword.trim());
  }

  return params.toString();
}

export function getBoardsPage(keyword = "", page = 0, size = 20): Promise<PageResponse<BoardListResponse>> {
  return request<PageResponse<BoardListResponse>>(`/api/boards?${createBoardQuery(keyword, page, size)}`);
}

export function getPublicBoard(boardId: number): Promise<BoardResponse> {
  return request<BoardResponse>(`/api/boards/${boardId}`);
}

export function getPrivateBoard(boardId: number, postPassword: string): Promise<BoardResponse> {
  return request<BoardResponse>(`/api/boards/${boardId}/password`, {
    method: "POST",
    body: JSON.stringify({ postPassword }),
  });
}

export function createBoard(requestBody: BoardSaveRequest): Promise<BoardResponse> {
  return request<BoardResponse>("/api/boards", {
    method: "POST",
    body: JSON.stringify(requestBody),
  });
}

export function updateBoard(boardId: number, requestBody: BoardSaveRequest): Promise<BoardResponse> {
  return request<BoardResponse>(`/api/boards/${boardId}`, {
    method: "PATCH",
    body: JSON.stringify(requestBody),
  });
}
