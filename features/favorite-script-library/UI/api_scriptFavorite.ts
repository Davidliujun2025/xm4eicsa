// src/api/scriptFavorite.ts

import { request } from '../utils/request';
import {
  FavoriteResponse,
  ToggleFavoriteRequest,
  ToggleFavoriteResponse,
  PageResponse,
} from '../types/favorite';

/**
 * 切换收藏状态（收藏或取消）
 */
export const toggleFavorite = (data: ToggleFavoriteRequest) => {
  return request<ToggleFavoriteResponse>({
    url: '/api/v1/script-favorites/toggle',
    method: 'POST',
    data,
  });
};

/**
 * 搜索话术列表（分页、关键词、标签过滤）
 */
export const searchFavorites = (
  keyword?: string,
  tag?: string,
  page: number = 0,
  size: number = 20
) => {
  return request<PageResponse<FavoriteResponse>>({
    url: '/api/v1/script-favorites',
    method: 'GET',
    params: { keyword, tag, page, size },
  });
};

/**
 * 标记话术为已使用（更新最后使用时间）
 */
export const markUsed = (favoriteId: string) => {
  return request<FavoriteResponse>({
    url: `/api/v1/script-favorites/${favoriteId}/use`,
    method: 'POST',
  });
};

/**
 * 获取当前用户的所有标签列表
 */
export const listTags = () => {
  return request<string[]>({
    url: '/api/v1/script-favorites/tags',
    method: 'GET',
  });
};

/**
 * 删除话术
 */
export const deleteFavorite = (favoriteId: string) => {
  return request<void>({
    url: `/api/v1/script-favorites/${favoriteId}`,
    method: 'DELETE',
  });
};