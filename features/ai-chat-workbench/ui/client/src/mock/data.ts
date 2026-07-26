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

export type Conv = {
  t: string;
  tag: string;
  time: string;
  platform?: string;
  messages?: { role: "user" | "assistant"; content: string; time?: string }[];
};

export const CONVS: Conv[] = [
  {
    t: "耳机支持七天无理由退货吗？",
    time: "10:24",
    tag: "售后咨询",
    messages: [
      { role: "user", content: "您好，请问这款蓝牙耳机支持七天无理由退货吗？", time: "10:20" },
      { role: "assistant", content: "您好，支持七天内无理由退货，只要商品未拆封且不影响二次销售。", time: "10:22" },
      { role: "user", content: "好的，那退货流程是怎样的？", time: "10:24" },
      { role: "assistant", content: "您可以在订单详情页点击“申请售后”进行操作，我们会尽快审核。", time: "10:25" },
    ]
  },
  {
    t: "什么时候发货？",
    time: "09:58",
    tag: "直接咨询",
    messages: [
      { role: "user", content: "我下单好几天了，什么时候能发货？", time: "09:50" },
      { role: "assistant", content: "很抱歉让您久等，我们会尽快安排发货，通常48小时内发出。", time: "09:52" },
      { role: "user", content: "能帮忙催一下吗？", time: "09:55" },
      { role: "assistant", content: "好的，我帮您备注加急，请耐心等待。", time: "09:58" },
    ]
  },
  {
    t: "如何申请退换货？",
    time: "昨天",
    tag: "售后咨询",
    messages: [
      { role: "user", content: "收到商品有质量问题，怎么申请退换货？", time: "昨天 14:20" },
      { role: "assistant", content: "非常抱歉给您带来不便，您可以在订单详情页点击“申请售后”，选择“退换货”并上传问题照片。", time: "昨天 14:22" },
      { role: "user", content: "需要自己承担运费吗？", time: "昨天 14:25" },
      { role: "assistant", content: "如果是质量问题，我们承担来回运费，您先垫付，我们会为您补偿。", time: "昨天 14:27" },
    ]
  },
  {
    t: "有优惠券吗？",
    time: "昨天",
    tag: "直接咨询",
    messages: [
      { role: "user", content: "现在购买有优惠券可以使用吗？", time: "昨天 10:10" },
      { role: "assistant", content: "目前店铺有满减券和新人券，您可以在首页领取。", time: "昨天 10:12" },
    ]
  },
  {
    t: "商品参数有误怎么办？",
    time: "昨天",
    tag: "售后咨询",
    messages: [
      { role: "user", content: "我看到商品参数里的尺寸和实际收到的不一样，怎么办？", time: "昨天 16:30" },
      { role: "assistant", content: "非常抱歉，可能是页面信息有误，我反馈给运营核实，同时您如果需要退货我们支持。", time: "昨天 16:32" },
    ]
  },
  {
    t: "充电线是否兼容？",
    time: "昨天",
    tag: "直接咨询",
    messages: [
      { role: "user", content: "这款耳机的充电线能和其他品牌通用吗？", time: "昨天 08:45" },
      { role: "assistant", content: "可以的，充电接口为Type-C，大部分安卓充电线都能兼容。", time: "昨天 08:47" },
    ]
  },
];

export const FILTERS = ["全部", "直接咨询", "售后咨询", "其他"] as const;
export const STEPS = ["意图识别", "回复策略", "推荐话术", "钩子引导", "成功收尾"];
export const NOTES = [
  "仔细理解客户诉求，拆解客户问题后，AI 将为您生成回复建议。",
  "AI 将结合相关话术和知识库内容生成回复。",
  "平台规则与政策可能发生变化，请遵守平台最新规范，谨慎回复。",
];