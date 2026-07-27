// src/App.tsx

import React from 'react';
import { Layout, Menu, Typography, Avatar, Space, Badge, Divider } from 'antd';
import {
  MessageOutlined,
  DatabaseOutlined,
  StarOutlined,
  SettingOutlined,
  LogoutOutlined,
  UserOutlined,
  DashboardOutlined,
} from '@ant-design/icons';
import AiCustomer from './views/aiCustomer';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

const App: React.FC = () => {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      {/* 侧边栏 */}
      <Sider
        width={220}
        style={{
          background: '#fff',
          borderRight: '1px solid #f0f0f0',
          boxShadow: '2px 0 8px rgba(0,0,0,0.05)',
        }}
      >
        <div style={{ padding: '20px 16px 16px', borderBottom: '1px solid #f0f0f0' }}>
          <Space>
            <div
              style={{
                width: 32,
                height: 32,
                borderRadius: 8,
                background: '#1677ff',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: '#fff',
                fontWeight: 'bold',
                fontSize: 16,
              }}
            >
              C
            </div>
            <Text strong style={{ fontSize: 16 }}>
              CarePilot AI
            </Text>
          </Space>
        </div>

        <Menu
          mode="inline"
          defaultSelectedKeys={['favorite']}
          style={{ borderRight: 0, marginTop: 8 }}
          items={[
            {
              key: 'workbench',
              icon: <MessageOutlined />,
              label: '智能对话工作台',
            },
            {
              key: 'dashboard',
              icon: <DashboardOutlined />,
              label: '数据看板', // 原"个人看板"已修改为"数据看板"
            },
            {
              key: 'favorite',
              icon: <StarOutlined />,
              label: '个人话术库',
            },
            {
              key: 'divider-1',
              type: 'divider',
            },
            {
              key: 'settings',
              icon: <SettingOutlined />,
              label: '系统设置',
            },
          ]}
        />

        {/* 底部用户信息 */}
        <div
          style={{
            position: 'absolute',
            bottom: 0,
            width: '100%',
            padding: '16px',
            borderTop: '1px solid #f0f0f0',
          }}
        >
          <Space>
            <Avatar icon={<UserOutlined />} />
            <div>
              <Text strong style={{ display: 'block', fontSize: 13 }}>
                张小情
              </Text>
              <Text type="secondary" style={{ fontSize: 12 }}>
                客服主管
              </Text>
            </div>
          </Space>
        </div>
      </Sider>

      {/* 右侧主区域 */}
      <Layout>
        {/* 顶部导航 */}
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            borderBottom: '1px solid #f0f0f0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            height: 56,
          }}
        >
          <div>
            <Text strong style={{ fontSize: 16 }}>
              个人话术库
            </Text>
            <Text type="secondary" style={{ marginLeft: 12, fontSize: 13 }}>
              / 收藏管理
            </Text>
          </div>
          <Space size={16}>
            <Badge dot>
              <Button type="text" icon={<SettingOutlined />} />
            </Badge>
            <Text type="secondary">|</Text>
            <Space>
              <Avatar size="small" icon={<UserOutlined />} />
              <Text>张小情</Text>
            </Space>
          </Space>
        </Header>

        {/* 主要内容 */}
        <Content style={{ background: '#f5f7fa' }}>
          <AiCustomer />
        </Content>
      </Layout>
    </Layout>
  );
};

export default App;