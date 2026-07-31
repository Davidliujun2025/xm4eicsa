import React, { useEffect, useMemo, useState } from 'react';
import AiCustomer from './views/aiCustomer';

type CurrentUser = {
  username: string;
};

const navItems = [
  { label: '智能对话', href: '/workbench/' },
  { label: '对话记录' },
  { label: '个人话术库', href: '/favorite-script-library/', active: true },
  { label: '我的评估' },
  { label: 'Token 统计', href: '/token-usage/' },
];

const App: React.FC = () => {
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const userName = currentUser?.username || '正在加载';
  const avatar = useMemo(() => userName.slice(0, 1), [userName]);

  useEffect(() => {
    fetch('/api/v1/auth/me', { credentials: 'include' })
      .then((response) => {
        if (response.status === 401) {
          window.location.href = `/?returnUrl=${encodeURIComponent(window.location.pathname)}`;
          throw new Error('unauthorized');
        }
        if (!response.ok) throw new Error('failed to load current user');
        return response.json();
      })
      .then((payload) => setCurrentUser(payload.data))
      .catch(() => setCurrentUser(null));
  }, []);

  return (
    <div className="script-app-shell">
      <aside className="script-sidebar">
        <div className="script-brand">
          <img src="/logo.png" alt="CarePilot AI" className="script-logo" />
        </div>
        <nav className="script-nav" aria-label="主导航">
          {navItems.map((item) => (
            <button
              key={item.label}
              className={`script-nav-item${item.active ? ' active' : ''}`}
              type="button"
              onClick={() => {
                if (item.href) window.location.href = item.href;
              }}
            >
              <span>{item.label}</span>
            </button>
          ))}
        </nav>
      </aside>

      <div className="script-main-shell">
        <header className="script-topbar">
          <div className="script-title-block">
            <h1>个人话术库</h1>
            <p>沉淀高质量回复话术，支持客服快速复用</p>
          </div>
          <div className="script-top-actions">
            <button className="script-account-button" type="button" aria-label="账号菜单">
              <span className="script-avatar">{avatar}</span>
              <span className="script-account-name">{userName}</span>
            </button>
          </div>
        </header>
        <main className="script-content"><AiCustomer /></main>
      </div>
    </div>
  );
};

export default App;
