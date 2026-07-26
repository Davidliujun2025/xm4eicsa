var mockData = {
  stats: {
    today: 2356,
    todayTrend: 8,
    week: 18623,
    weekTrend: 3,
    month: 52890,
    monthTrend: 12,
    history: 126532
  },
  trendData: [
    {date:'7/1', inputToken:320, outputToken:120, totalToken:440},
    {date:'7/2', inputToken:560, outputToken:40, totalToken:600},
    {date:'7/3', inputToken:380, outputToken:60, totalToken:440},
    {date:'7/4', inputToken:800, outputToken:20, totalToken:820},
    {date:'7/5', inputToken:560, outputToken:220, totalToken:780},
    {date:'7/6', inputToken:860, outputToken:120, totalToken:980},
    {date:'7/7', inputToken:620, outputToken:120, totalToken:740}
  ],
  weeklyTrendData: [
    {date:'第1周', inputToken:3400, outputToken:980, totalToken:4380},
    {date:'第2周', inputToken:3800, outputToken:1120, totalToken:4920},
    {date:'第3周', inputToken:4100, outputToken:900, totalToken:5000},
    {date:'第4周', inputToken:3600, outputToken:1040, totalToken:4640}
  ],
  monthlyTrendData: [
    {date:'1月', inputToken:14000, outputToken:4200, totalToken:18200},
    {date:'2月', inputToken:13200, outputToken:3800, totalToken:17000},
    {date:'3月', inputToken:15600, outputToken:4400, totalToken:20000},
    {date:'4月', inputToken:14900, outputToken:4100, totalToken:19000}
  ],
  records: [
    {id:'r1', callTime:'2026-07-07 10:32', modelName:'GPT-4o', inputToken:120, outputToken:260, totalToken:380, responseTime:120, status:'success', sessionId:'s1', userId:'u1', prompt:'示例输入1', response:'示例响应1'},
    {id:'r2', callTime:'2026-07-07 09:18', modelName:'GPT-4o-mini', inputToken:80, outputToken:140, totalToken:220, responseTime:110, status:'success', sessionId:'s2', userId:'u1', prompt:'示例输入2', response:'示例响应2'},
    {id:'r3', callTime:'2026-07-06 21:45', modelName:'GPT-4o', inputToken:200, outputToken:310, totalToken:510, responseTime:200, status:'success', sessionId:'s3', userId:'u2', prompt:'示例输入3', response:'示例响应3'},
    {id:'r4', callTime:'2026-07-06 16:20', modelName:'GPT-4o-mini', inputToken:60, outputToken:120, totalToken:180, responseTime:90, status:'success', sessionId:'s4', userId:'u1', prompt:'示例输入4', response:'示例响应4'},
    {id:'r5', callTime:'2026-07-05 14:05', modelName:'GPT-4o', inputToken:150, outputToken:240, totalToken:390, responseTime:130, status:'success', sessionId:'s5', userId:'u3', prompt:'示例输入5', response:'示例响应5'}
  ]
};
