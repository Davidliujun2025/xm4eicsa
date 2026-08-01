export type ChatMessage = {
  id: number;
  dialogRound: number;
  question: string;
  customerType?: string;
  intentRecognition?: string;
  replyStrategy?: string;
  recommendedScript?: string;
  hookGuidance?: string;
  successClose?: string;
  riskWarning?: string;
  riskSuggestion?: string;
  totalTokens: number;
  promptTokens: number;
  completionTokens: number;
  createdAt?: string;
};

export type TokenInfo = {
  currentChatUsage: number;
  totalLimit: number;
  usedToday: number;
  usagePercent: number;
};

export type Conversation = {
  conversationId: string;
  sessionTaskId: number;
  platform: string;
  title: string;
  messages: ChatMessage[];
  tokenInfo: TokenInfo;
  createdAt?: string;
  updatedAt?: string;
};

export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};
