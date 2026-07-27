export interface FavoriteResponse {
  id: string;
  sourceTalkId: string | null;
  content: string;
  scenario: string;
  generatedAt: string;
  tags: string[];
  createdAt: string;
  lastUsedAt: string;
}

export interface ToggleFavoriteRequest {
  sourceTalkId?: string;
  content: string;
  scenario: string;
  generatedAt?: string;
  tags: string[];
}

export interface ToggleFavoriteResponse {
  favorited: boolean;
  favorite: FavoriteResponse | null;
  message: string;
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
}

export interface ErrorResponse {
  message: string;
  errors: Record<string, string>;
  timestamp: string;
}