import React, { useEffect, useRef, useState } from 'react';
import AiCustomer from './views/aiCustomer';
import { logoutAndRedirect } from './utils/auth';

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
    <path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    <circle cx="12" cy="12" r="3" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
  </svg>
);

const navItems = [
  { Icon: Chat, label: '智能对话' },
  { Icon: Clock, label: '对话记录' },
  { Icon: Book, label: '个人话术库' },
  { Icon: Clip, label: '我的评估' },
];

const isLocalDevelopment = () => window.location.hostname === '127.0.0.1' || window.location.hostname === 'localhost';
const localUrl = (port: number, path = '/') => `${window.location.protocol}//${window.location.hostname}:${port}${path}`;

const getAppLinks = (): Record<string, string> => {
  if (isLocalDevelopment()) {
    const workbenchUrl = import.meta.env.VITE_WORKBENCH_URL || localUrl(15174);
    const tokenUsageUrl = import.meta.env.VITE_TOKEN_USAGE_URL || localUrl(15280);
    return {
      '智能对话': workbenchUrl,
      '对话记录': `${workbenchUrl.replace(/\/$/, '')}/?view=history`,
      '个人话术库': window.location.href,
      '我的评估': `${workbenchUrl.replace(/\/$/, '')}/?view=evaluation`,
      'Token统计': tokenUsageUrl,
      '设置': `${workbenchUrl.replace(/\/$/, '')}/?view=setting`,
    };
  }

  return {
    '智能对话': `${window.location.origin}/workbench/`,
    '对话记录': `${window.location.origin}/workbench/?view=history`,
    '个人话术库': `${window.location.origin}/favorite-script-library/`,
    '我的评估': `${window.location.origin}/workbench/?view=evaluation`,
    'Token统计': `${window.location.origin}/token-usage/`,
    '设置': `${window.location.origin}/workbench/?view=setting`,
  };
};

const App: React.FC = () => {
  // ========== 完全对齐Sidebar的状态持久化逻辑 ==========
  const [expandState, setExpandState] = useState<Record<string, boolean>>(() => {
    const saved = localStorage.getItem("sidebar_expand");
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch {
        return {};
      }
    }
    return { dashboard: true };
  });
  const dashboardOpen = expandState.dashboard ?? true;

  // 状态同步存入localStorage
  useEffect(() => {
    localStorage.setItem("sidebar_expand", JSON.stringify(expandState));
  }, [expandState]);

  // 切换函数和Sidebar保持一致
  const toggleDashboard = () => {
    setExpandState(prev => ({
      ...prev,
      dashboard: !prev.dashboard
    }));
  };

  const [displayName, setDisplayName] = useState('客服');
  const appLinks = getAppLinks();

  const [userMenuOpen, setUserMenuOpen] = useState(false);
  const [accountModalType, setAccountModalType] = useState<'' | 'logout' | 'switch'>('');
  const [accountSubmitting, setAccountSubmitting] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (userMenuRef.current && !userMenuRef.current.contains(e.target as Node)) {
        setUserMenuOpen(false);
      }
    };
    document.addEventListener('click', handleClickOutside);
    return () => document.removeEventListener('click', handleClickOutside);
  }, []);

  const openUserMenu = () => setUserMenuOpen((v) => !v);
  const openSwitchModal = () => {
    setUserMenuOpen(false);
    setAccountModalType('switch');
  };
  const openLogoutModal = () => {
    setUserMenuOpen(false);
    setAccountModalType('logout');
  };
  const closeAccountModal = () => setAccountModalType('');
  const confirmAccountAction = async () => {
    setAccountSubmitting(true);
    try {
      await logoutAndRedirect(window.location.href);
    } finally {
      setAccountSubmitting(false);
    }
  };

  useEffect(() => {
    const loadCurrentUser = async () => {
      try {
        const response = await fetch('/api/v1/auth/me', { credentials: 'include' });
        if (!response.ok) return;
        const payload = await response.json();
        setDisplayName(payload?.data?.username || payload?.data?.account || '客服');
      } catch {
        // Keep a neutral label when account details are temporarily unavailable.
      }
    };
    void loadCurrentUser();
  }, []);

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

  {/* 数据看板父菜单 */}
  <button className="script-nav-item script-nav-item-fold" type="button" onClick={toggleDashboard}>
    <span className="script-nav-item-main">
      <Chart className="script-nav-icon" />
      <span>数据看板</span>
    </span>
    <Chev
      className={`script-fold-arrow script-chev-icon ${dashboardOpen ? 'rotate-180' : ''}`}
    />
  </button>

  {/* Token统计 文字前增加两个空格 */}
  {dashboardOpen && (
  <button
    className="script-nav-item script-nav-child-item"
    type="button"
    onClick={() => {
      window.location.href = appLinks['Token统计'];
    }}
  >
    Token统计
  </button>
)}

  <button className="script-nav-item settings-item" type="button" onClick={() => {
    const target = appLinks['设置'];
    if (target) window.location.href = target;
  }}>
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
            <div className="script-user-menu" ref={userMenuRef}>
              <button
                className="script-account-button"
                type="button"
                aria-label="账号菜单"
                onClick={openUserMenu}
              >
                <span className="script-avatar">{displayName.trim().slice(0, 1) || '客'}</span>
                <span className="script-account-name">{displayName}</span>
                <Chev className={`script-chev-icon${userMenuOpen ? ' is-open' : ''}`} />
              </button>

              {userMenuOpen && (
                <div className="script-user-dropdown">
                  <button type="button" onClick={openSwitchModal}>切换账号</button>
                  <button type="button" onClick={openLogoutModal}>退出登录</button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="script-content">
          <AiCustomer />
        </main>
      </div>

      {accountModalType && (
        <div className="script-modal-overlay">
          <div className="script-modal-box">
            <h3>{accountModalType === 'logout' ? '确认退出登录' : '确认切换账号'}</h3>
            <div className="script-modal-actions">
              <button type="button" onClick={closeAccountModal} disabled={accountSubmitting}>
                否
              </button>
              <button type="button" onClick={() => void confirmAccountAction()} disabled={accountSubmitting}>
                {accountSubmitting ? '处理中...' : '是'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default App;
