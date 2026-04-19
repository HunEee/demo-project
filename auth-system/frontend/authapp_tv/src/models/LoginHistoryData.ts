import type LoginHistory from "./LoginHistory.ts";

export default interface LoginHistoryResponse {
  content: LoginHistory[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}