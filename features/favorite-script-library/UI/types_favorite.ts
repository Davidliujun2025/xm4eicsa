// src/types/favorite.ts

/**
 * 收藏话术响应（单个话术详情）
 */
export interface FavoriteResponse {
  id: string; // UUID
  sourceTalkId: string | null;
  content: string;
  scenario: string;
  generatedAt: string; // ISO 8601
  tags: string[];
  createdAt: string;
  lastUsedAt: string;
}

/**
 * 收藏/取消收藏请求体
 */
export interface ToggleFavoriteRequest {
  sourceTalkId?: string; // 最大128字符，可选
  content: string; // 必填
  scenario: string; // 必填，最大100字符
  generatedAt?: string; // ISO 8601，可选
  tags: string[]; // 必填，至少1个，每个最多5字符
}

/**
 * 收藏/取消收藏响应
 */
export interface ToggleFavoriteResponse {
  favorited: boolean;
  favorite: FavoriteResponse | null;
  message: string;
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

/**
 * 错误响应
 */
export interface ErrorResponse {
  message: string;
  errors: Record<string, string>;
  timestamp: string;
}