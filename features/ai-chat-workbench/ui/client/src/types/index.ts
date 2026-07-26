// 定义对话记录的类型
export type Conversation = {
  id: number;
  question: string;
  platform: string;
  category: string;
  time: string;
};

// 定义 AI 生成步骤的类型
export type GenerationStatus = "idle" | "generating" | "slow" | "success" | "failed";

// 定义话术步骤卡片的类型
export type StepCard = {
  title: string;
  label: string;
  tags: [string, string];
  section: string;
  body: string;
  status: "done" | "active" | "pending";
};