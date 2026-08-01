export type Platform = {
  id: string;
  backendValue: string;
  name: string;
  color: string;
  shortName: string;
};

export const PLATFORMS: Platform[] = [
  { id: "tb", backendValue: "TAOBAO", name: "淘宝", color: "#ff5000", shortName: "淘" },
  { id: "tm", backendValue: "TMALL", name: "天猫", color: "#ff0036", shortName: "猫" },
  { id: "jd", backendValue: "JD", name: "京东", color: "#e1251b", shortName: "京" },
  { id: "pdd", backendValue: "PINDUODUO", name: "拼多多", color: "#e02e24", shortName: "拼" },
  { id: "dy", backendValue: "DOUYIN", name: "抖音", color: "#161823", shortName: "抖" },
  { id: "xhs", backendValue: "XIAOHONGSHU", name: "小红书", color: "#ff2741", shortName: "红" },
  { id: "ks", backendValue: "KUAISHOU", name: "快手", color: "#ff4903", shortName: "快" },
  { id: "sph", backendValue: "VIDEO_NUMBER", name: "视频号", color: "#fa9d3b", shortName: "视" },
  { id: "wx", backendValue: "WECHAT_SHOP", name: "微信小店", color: "#07c160", shortName: "微" },
  { id: "ot", backendValue: "OTHER", name: "其他平台", color: "#9aa3b2", shortName: "…" },
];

export const FILTERS = ["全部", "直接咨询", "售后咨询", "其他"] as const;
export const STEPS = ["意图识别", "回复策略", "推荐话术", "钩子引导", "成功收尾"] as const;
export const NOTES = [
  "仔细理解客户诉求，拆解客户问题后，AI 将为您生成回复建议。",
  "AI 将结合相关话术和知识库内容生成回复。",
  "平台规则与政策可能发生变化，请遵守平台最新规范，谨慎回复。",
] as const;

export function platformFor(value?: string | null) {
  if (!value) return PLATFORMS[1];
  return PLATFORMS.find((platform) =>
    [platform.id, platform.name, platform.backendValue].some(
      (candidate) => candidate.toLowerCase() === value.toLowerCase(),
    ),
  ) ?? PLATFORMS[9];
}
