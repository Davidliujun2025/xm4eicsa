import type { Conversation } from "../types";

const INACTIVITY_TIMEOUT_MS = 25 * 60 * 1000;

export function isHistoryConversation(conversation: Conversation, now = Date.now()) {
  if (conversation.status) return conversation.status === "HISTORY";

  // 滚动升级兼容：旧后端没有 status 时，只依据原有数据识别明确已完成或已超时的会话。
  if (conversation.messages?.at(-1)?.successClose?.trim()) return true;
  const activityAt = conversation.updatedAt || conversation.createdAt;
  if (!activityAt) return false;
  const activityTime = new Date(activityAt).getTime();
  return !Number.isNaN(activityTime) && now - activityTime >= INACTIVITY_TIMEOUT_MS;
}
