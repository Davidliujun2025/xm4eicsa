import type { ApiResponse, Conversation, TokenInfo } from "../types";

type ConversationInput = {
  question: string;
  platform: string;
  customerType?: string;
};

type StepGenerationInput = {
  carrierName?: string;
  intentRecognition?: string;
  customerQuestion?: string;
  platform?: string;
};

type StepContentUpdateInput = {
  content: string;
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

async function readJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();
  if (!text.trim()) {
    return null;
  }
  try {
    return JSON.parse(text) as T;
  } catch {
    return null;
  }
}

function responseError(response: Response, message?: string) {
  if (message) {
    return message;
  }
  if (response.status >= 500) {
    return "暂时无法连接后端服务，请确认后端已启动后重试";
  }
  return "服务请求失败，请稍后重试";
}

async function parseResponse<T>(response: Response): Promise<T> {
  const payload = await readJson<ApiResponse<T>>(response);
  if (!response.ok || !payload || payload.code !== 200) {
    throw new Error(responseError(response, payload?.message));
  }
  return payload.data;
}

async function csrfToken(signal?: AbortSignal) {
  const response = await fetch("/api/v1/auth/csrf", { credentials: "include", signal });
  const payload = await readJson<{ data?: { token?: string }; message?: string }>(response);
  if (!response.ok || !payload?.data?.token) {
    throw new Error(responseError(response, payload?.message || (response.ok ? "无法获取安全令牌" : undefined)));
  }
  return payload.data.token;
}

async function mutate<T>(url: string, method: "POST" | "PUT" | "DELETE", body?: unknown, signal?: AbortSignal) {
  const token = await csrfToken(signal);
  return parseResponse<T>(await fetch(url, {
    method,
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
      "X-XSRF-TOKEN": token,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
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
  const payload = await readJson<T & { message?: string }>(response);
  if (!response.ok || !payload) {
    throw new Error(responseError(response, payload?.message));
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

  async archiveConversation(conversationId: string) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}/archive`,
      "POST",
    );
  },

  async regenerateStep(conversationId: string, dialogRound: number, stepNo: number, input?: StepGenerationInput, signal?: AbortSignal) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`
        + `/dialogs/${dialogRound}/steps/${stepNo}/regenerate`,
      "POST",
      input,
      signal,
    );
  },

  async generateStep(conversationId: string, dialogRound: number, stepNo: number, input?: StepGenerationInput, signal?: AbortSignal) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`
        + `/dialogs/${dialogRound}/steps/${stepNo}/generate`,
      "POST",
      input,
      signal,
    );
  },

  async updateStepContent(conversationId: string, dialogRound: number, stepNo: number, input: StepContentUpdateInput) {
    return mutate<Conversation>(
      `/api/v1/conversations/${encodeURIComponent(conversationId)}`
        + `/dialogs/${dialogRound}/steps/${stepNo}/content`,
      "PUT",
      input,
    );
  },

  async togglePersonalFavorite(input: PersonalFavoriteToggleInput) {
    return mutateJson<PersonalFavoriteToggleResponse>("/api/v1/script-favorites/toggle", "POST", input);
  },
};
