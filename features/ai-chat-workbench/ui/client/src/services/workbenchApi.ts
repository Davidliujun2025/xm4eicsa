import type { ApiResponse, Conversation, TokenInfo } from "../types";

type ConversationInput = {
  question: string;
  platform: string;
  customerType?: string;
};

type PersonalFavoriteToggleInput = {
  sourceTalkId?: string;
  content: string;
  scenario: string;
  generatedAt?: string;
  tags: string[];
};

type PersonalFavoriteToggleResponse = {
  favorited: boolean;
  favorite: {
    id: string;
  } | null;
  message: string;
};

async function parseResponse<T>(response: Response): Promise<T> {
  const payload = await response.json() as ApiResponse<T>;
  if (!response.ok || payload.code !== 200) {
    throw new Error(payload.message || "服务请求失败");
  }
  return payload.data;
}

async function csrfToken() {
  const response = await fetch("/api/v1/auth/csrf", { credentials: "include" });
  const payload = await response.json() as { data?: { token?: string }; message?: string };
  if (!response.ok || !payload.data?.token) {
    throw new Error(payload.message || "无法获取安全令牌");
  }
  return payload.data.token;
}

async function mutate<T>(url: string, method: "POST" | "DELETE", body?: unknown) {
  const token = await csrfToken();
  return parseResponse<T>(await fetch(url, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  }));
}

async function mutateJson<T>(url: string, method: "POST" | "DELETE", body?: unknown) {
  const token = await csrfToken();
  const response = await fetch(url, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const payload = await response.json() as T & { message?: string };
  if (!response.ok) {
    throw new Error(payload?.message || "服务请求失败");
  }
  return payload;
}

export const workbenchApi = {
  async listConversations() {
    return parseResponse<Conversation[]>(await fetch("/api/v1/conversations", {
      credentials: "include",
    }));
  },

  async getConversation(conversationId: string) {
    return parseResponse<Conversation>(await fetch(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`,
      { credentials: "include" },
    ));
  },

  async getTokenUsage() {
    return parseResponse<TokenInfo>(await fetch("/api/v1/tokens/usage", {
      credentials: "include",
    }));
  },

  async createConversation(input: ConversationInput) {
    return mutate<Conversation>("/api/v1/conversations", "POST", input);
  },

  async addMessage(conversationId: string, input: ConversationInput) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}/messages`,
      "POST",
      input,
    );
  },

  async regenerateStep(conversationId: string, dialogRound: number, stepNo: number) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`
        + `/dialogs/${dialogRound}/steps/${stepNo}/regenerate`,
      "POST",
    );
  },

  async generateStep(conversationId: string, dialogRound: number, stepNo: number) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`
        + `/dialogs/${dialogRound}/steps/${stepNo}/generate`,
      "POST",
    );
  },

  async togglePersonalFavorite(input: PersonalFavoriteToggleInput) {
    return mutateJson<PersonalFavoriteToggleResponse>("/api/v1/script-favorites/toggle", "POST", input);
  },
};
