export type Platform = {
  id: string;
  name: string;
  c: string;
  t: string;
  dark?: boolean;
};

export const PLATFORMS: Platform[] = [
  { id: "tb", name: "淘宝", c: "#ff5000", t: "淘" },
  { id: "tm", name: "天猫", c: "#ff0036", t: "猫", dark: true },
  { id: "jd", name: "京东", c: "#e1251b", t: "京" },
  { id: "pdd", name: "拼多多", c: "#e02e24", t: "拼" },
  { id: "dy", name: "抖音", c: "#161823", t: "抖" },
  { id: "xhs", name: "小红书", c: "#ff2741", t: "红" },
  { id: "ks", name: "快手", c: "#ff4903", t: "快" },
  { id: "sph", name: "视频号", c: "#fa9d3b", t: "视" },
  { id: "wx", name: "微信小店", c: "#07c160", t: "微" },
  { id: "ot", name: "其他平台", c: "#9aa3b2", t: "…" },
];

export type Conv = { t: string; tag: string; time: string };

export const CONVS: Conv[] = [
  { t: "耳机支持七天无理由退货吗?", tag: "售后咨询", time: "10:24" },
  { t: "什么时候发货?", tag: "直接咨询", time: "09:58" },
  { t: "如何申请退换货?", tag: "售后咨询", time: "昨天" },
  { t: "有优惠券吗?", tag: "直接咨询", time: "昨天" },
  { t: "商品参数有误怎么办?", tag: "售后咨询", time: "昨天" },
  { t: "充电线是否兼容?", tag: "直接咨询", time: "昨天" },
  { t: "如何联系售后?", tag: "售后咨询", time: "2天前" },
  { t: "能开发票吗?", tag: "其他", time: "2天前" },
];

export const FILTERS = ["全部", "直接咨询", "售后咨询", "其他"] as const;

export const STEPS = ["意图识别", "回复策略", "推荐话术", "钩子引导", "成功收尾"];

export const NOTES = [
  "仔细理解客户诉求，拆解客户问题后，AI 将为您生成回复建议。",
  "AI 将结合相关话术和知识库内容生成回复。",
  "平台规则与政策可能发生变化，请遵守平台最新规范，谨慎回复。",
];