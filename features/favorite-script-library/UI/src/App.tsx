import React, { useState } from 'react';
import AiCustomer from './views/aiCustomer';

type IconProps = { className?: string };

const Chat = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <path d="M4 5h16v11H8l-4 3V5Z" stroke="currentColor" strokeWidth="1.7" strokeLinejoin="round" />
  </svg>
);

const Clock = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="M12 7.5V12l3 2" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

const Book = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H12v16H6.5A2.5 2.5 0 0 0 4 21.5V5.5ZM20 5.5A2.5 2.5 0 0 0 17.5 3H12v16h5.5a2.5 2.5 0 0 1 2.5 2.5V5.5Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
  </svg>
);

const Clip = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <rect x="5" y="4" width="14" height="17" rx="2.5" stroke="currentColor" strokeWidth="1.7" />
    <path d="M9 4h6v2.5H9zM9 12l2 2 4-4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const Chart = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <path d="M4 20V4M4 20h16M8 16v-4M12 16V8M16 16v-6" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" />
  </svg>
);

const Help = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <circle cx="12" cy="12" r="8.5" stroke="currentColor" strokeWidth="1.6" />
    <path d="M9.5 9.5a2.5 2.5 0 1 1 3.5 2.3c-.8.4-1 .9-1 1.7M12 16.5h.01" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
  </svg>
);

const Bell = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6ZM10 19a2 2 0 0 0 4 0" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
  </svg>
);

const Chev = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <path d="m6 9 6 6 6-6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const Settings = (props: IconProps) => (
  <svg viewBox="0 0 24 24" fill="none" className={props.className}>
    <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33h.01a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h.01a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v.01a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const navItems = [
  { Icon: Chat, label: '智能对话' },
  { Icon: Clock, label: '对话记录' },
  { Icon: Book, label: '个人话术库' },
  { Icon: Clip, label: '我的评估' },
];

const appLinks: Record<string, string> = {
  '智能对话': 'http://localhost:5174/',
  '个人话术库': 'http://localhost:5176/',
  'Token统计': 'http://localhost:5600/'
};

const App: React.FC = () => {
  const [dashboardOpen, setDashboardOpen] = useState(true);

  return (
    <div className="script-app-shell">
      <aside className="script-sidebar">
        <div className="script-brand">
          <img src="/logo.png" alt="CarePilot AI" className="script-logo" />
        </div>
        <nav className="script-nav" aria-label="主导航">
          {navItems.map(({ Icon, label }, index) => {
            const active = index === 2;
            return (
              <button
                key={label}
                className={`script-nav-item${active ? ' active' : ''}`}
                type="button"
                onClick={() => {
                  const target = appLinks[label];
                  if (target) window.location.href = target;
                }}
              >
                <Icon className="script-nav-icon" />
                <span>{label}</span>
              </button>
            );
          })}
          <button className="script-nav-item script-nav-item-fold" type="button" onClick={() => setDashboardOpen((v) => !v)}>
            <span className="script-nav-item-main">
              <Chart className="script-nav-icon" />
              <span>数据看板</span>
            </span>
            <span className={`script-fold-arrow${dashboardOpen ? '' : ' collapsed'}`}>▼</span>
          </button>
          <button
            className={`script-nav-item script-nav-child-item${dashboardOpen ? '' : ' hidden'}`}
            type="button"
            onClick={() => {
              window.location.href = appLinks['Token统计'];
            }}
          >
            <span>Token统计</span>
          </button>
          <button className="script-nav-item settings-item" type="button">
            <Settings className="script-nav-icon" />
            <span>设置</span>
          </button>
        </nav>
      </aside>

      <div className="script-main-shell">
        <header className="script-topbar">
          <div className="script-title-block">
            <h1>个人话术库</h1>
            <p>沉淀高质量回复话术，支持客服快速复用</p>
          </div>

          <div className="script-top-actions" aria-label="用户工具栏">
            <button className="script-top-link" type="button">
              <Help className="script-help-icon" />
              <span>帮助中心</span>
            </button>
            <button className="script-bell-button" type="button" aria-label="通知消息">
              <Bell className="script-bell-icon" />
              <span>2</span>
            </button>
            <button className="script-account-button" type="button" aria-label="账号菜单">
              <span className="script-avatar">倩</span>
              <span className="script-account-name">张小倩</span>
              <Chev className="script-chev-icon" />
            </button>
          </div>
        </header>

        <main className="script-content">
          <AiCustomer />
        </main>
      </div>
    </div>
  );
};

export default App;