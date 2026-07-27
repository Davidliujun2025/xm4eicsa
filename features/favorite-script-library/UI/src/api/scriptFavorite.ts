import { request } from '../utils/request';
import type {
  FavoriteResponse,
  ToggleFavoriteRequest,
  ToggleFavoriteResponse,
  PageResponse,
} from '../types/favorite';

export const toggleFavorite = (data: ToggleFavoriteRequest) => {
  return request<ToggleFavoriteResponse>({
    url: '/api/v1/script-favorites/toggle',
    method: 'POST',
    data,
  });
};

export const searchFavorites = (
  keyword?: string,
  tag?: string,
  page = 0,
  size = 20
) => {
  return request<PageResponse<FavoriteResponse>>({
    url: '/api/v1/script-favorites',
    method: 'GET',
    params: { keyword, tag, page, size },
  });
};

export const markUsed = (favoriteId: string) => {
  return request<FavoriteResponse>({
    url: `/api/v1/script-favorites/${favoriteId}/use`,
    method: 'POST',
  });
};

export const listTags = () => {
  return request<string[]>({
    url: '/api/v1/script-favorites/tags',
    method: 'GET',
  });
};

export const deleteFavorite = (favoriteId: string) => {
  return request<void>({
    url: `/api/v1/script-favorites/${favoriteId}`,
    method: 'DELETE',
  });
};