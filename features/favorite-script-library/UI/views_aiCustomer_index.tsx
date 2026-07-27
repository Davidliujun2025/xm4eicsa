// src/views/aiCustomer/index.tsx

import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Input,
  Button,
  Select,
  Space,
  List,
  Tag,
  Pagination,
  Modal,
  Form,
  message,
  Empty,
  Tooltip,
  Typography,
  Spin,
} from 'antd';
import {
  SearchOutlined,
  PlusOutlined,
  DeleteOutlined,
  StarOutlined,
  StarFilled,
  CheckCircleOutlined,
} from '@ant-design/icons';
import {
  searchFavorites,
  toggleFavorite,
  markUsed,
  deleteFavorite,
  listTags,
} from '../../api/scriptFavorite';
import type { FavoriteResponse, ToggleFavoriteRequest } from '../../types/favorite';

const { TextArea } = Input;
const { Text } = Typography;

const AiCustomer: React.FC = () => {
  // ==================== State ====================
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState<FavoriteResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [size] = useState(12); // 每页12条，适合卡片展示
  const [keyword, setKeyword] = useState('');
  const [selectedTag, setSelectedTag] = useState<string | undefined>(undefined);
  const [tagOptions, setTagOptions] = useState<string[]>([]);

  // 收藏弹窗
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [tagInput, setTagInput] = useState('');
  const [tempTags, setTempTags] = useState<string[]>([]);

  // ==================== Data Fetching ====================
  const fetchList = useCallback(async () => {
    setLoading(true);
    try {
      const res = await searchFavorites(keyword || undefined, selectedTag, page, size);
      setList(res.items);
      setTotal(res.total);
    } catch (error: any) {
      message.error(error.message || '获取话术列表失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, selectedTag, page, size]);

  const fetchTags = useCallback(async () => {
    try {
      const res = await listTags();
      setTagOptions(res);
    } catch (error) {
      // 静默失败，不影响主流程
    }
  }, []);

  useEffect(() => {
    fetchList();
  }, [fetchList]);

  useEffect(() => {
    fetchTags();
  }, [fetchTags]);

  // ==================== Handlers ====================
  const handleSearch = () => {
    setPage(0);
    fetchList();
  };

  const handlePageChange = (newPage: number, newPageSize: number) => {
    setPage(newPage - 1); // Ant Design页码从1开始，后端从0开始
  };

  // 使用话术（更新最后使用时间）
  const handleUse = async (id: string) => {
    try {
      await markUsed(id);
      message.success('已标记使用');
      fetchList(); // 刷新列表（排序会变化）
    } catch (error: any) {
      message.error(error.message || '标记使用失败');
    }
  };

  // 删除话术
  const handleDelete = async (id: string, content: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除话术：「${content.length > 20 ? content.slice(0, 20) + '...' : content}」吗？`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await deleteFavorite(id);
          message.success('删除成功');
          fetchList();
        } catch (error: any) {
          message.error(error.message || '删除失败');
        }
      },
    });
  };

  // ==================== 收藏弹窗逻辑 ====================
  const openModal = () => {
    form.resetFields();
    setTempTags([]);
    setTagInput('');
    setModalVisible(true);
  };

  const handleAddTag = () => {
    const val = tagInput.trim();
    if (!val) {
      message.warning('请输入标签内容');
      return;
    }
    if (val.length > 5) {
      message.warning('标签最多5个字符');
      return;
    }
    if (tempTags.includes(val)) {
      message.warning('标签已存在');
      return;
    }
    setTempTags([...tempTags, val]);
    setTagInput('');
  };

  const handleRemoveTag = (tag: string) => {
    setTempTags(tempTags.filter((t) => t !== tag));
  };

  const handleSubmitFavorite = async () => {
    try {
      const values = await form.validateFields();
      if (tempTags.length === 0) {
        message.warning('请至少添加1个标签');
        return;
      }
      setSubmitting(true);
      const payload: ToggleFavoriteRequest = {
        content: values.content.trim(),
        scenario: values.scenario.trim(),
        tags: tempTags,
        sourceTalkId: values.sourceTalkId || undefined,
        generatedAt: values.generatedAt ? new Date(values.generatedAt).toISOString() : undefined,
      };
      const res = await toggleFavorite(payload);
      if (res.favorited) {
        message.success(res.message || '收藏成功');
        setModalVisible(false);
        fetchList();
        fetchTags(); // 刷新标签列表
      } else {
        message.info(res.message || '已取消收藏');
        setModalVisible(false);
      }
    } catch (error: any) {
      if (error.errors) {
        // 字段校验错误
        const errMsg = Object.values(error.errors).join('；');
        message.error(errMsg);
      } else {
        message.error(error.message || '操作失败');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // ==================== Render ====================
  return (
    <div style={{ padding: '24px', background: '#f5f7fa', minHeight: 'calc(100vh - 64px)' }}>
      {/* 页面标题 */}
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>个人话术库</h2>
        <Text type="secondary">管理收藏的AI话术，快速复用提升效率</Text>
      </div>

      {/* 搜索与操作栏 */}
      <Card style={{ marginBottom: 24 }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap>
            <Input
              placeholder="搜索话术内容或标签"
              prefix={<SearchOutlined />}
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 260 }}
              allowClear
            />
            <Select
              placeholder="筛选标签"
              allowClear
              value={selectedTag}
              onChange={(val) => setSelectedTag(val || undefined)}
              style={{ width: 150 }}
              options={tagOptions.map((tag) => ({ label: tag, value: tag }))}
            />
            <Button type="primary" onClick={handleSearch}>
              搜索
            </Button>
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openModal}>
            收藏话术
          </Button>
        </Space>
      </Card>

      {/* 话术卡片列表 */}
      <Spin spinning={loading}>
        {list.length === 0 ? (
          <Empty description="暂无话术，快去收藏第一条吧！" style={{ marginTop: 60 }} />
        ) : (
          <List
            grid={{ gutter: 16, xs: 1, sm: 2, md: 2, lg: 3, xl: 3, xxl: 4 }}
            dataSource={list}
            renderItem={(item) => (
              <List.Item>
                <Card
                  hoverable
                  style={{ height: '100%', display: 'flex', flexDirection: 'column' }}
                  bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column' }}
                  actions={[
                    <Tooltip title="标记使用（置顶排序）">
                      <Button
                        type="text"
                        icon={<CheckCircleOutlined />}
                        onClick={() => handleUse(item.id)}
                        style={{ color: '#52c41a' }}
                      >
                        使用
                      </Button>
                    </Tooltip>,
                    <Tooltip title="取消收藏">
                      <Button
                        type="text"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => handleDelete(item.id, item.content)}
                      >
                        删除
                      </Button>
                    </Tooltip>,
                  ]}
                >
                  <div style={{ marginBottom: 12, flex: 1 }}>
                    <div style={{ marginBottom: 8 }}>
                      <Text ellipsis={{ tooltip: item.content }} style={{ fontSize: 14 }}>
                        {item.content}
                      </Text>
                    </div>
                    <div style={{ marginBottom: 6 }}>
                      <Tag color="blue">{item.scenario}</Tag>
                    </div>
                    <div style={{ marginBottom: 6 }}>
                      {item.tags.map((tag) => (
                        <Tag key={tag} color="cyan" style={{ marginBottom: 4 }}>
                          {tag}
                        </Tag>
                      ))}
                    </div>
                  </div>
                  <div style={{ borderTop: '1px solid #f0f0f0', paddingTop: 8, fontSize: 12, color: '#999' }}>
                    <div>最近使用：{item.lastUsedAt ? new Date(item.lastUsedAt).toLocaleString() : '未使用'}</div>
                    <div>收藏于：{new Date(item.createdAt).toLocaleDateString()}</div>
                  </div>
                </Card>
              </List.Item>
            )}
          />
        )}
      </Spin>

      {total > 0 && (
        <div style={{ textAlign: 'right', marginTop: 24 }}>
          <Pagination
            current={page + 1}
            pageSize={size}
            total={total}
            onChange={handlePageChange}
            showSizeChanger={false}
            showQuickJumper
          />
        </div>
      )}

      {/* ==================== 收藏弹窗 Modal ==================== */}
      <Modal
        title={
          <Space>
            <StarFilled style={{ color: '#faad14' }} />
            <span>收藏话术</span>
          </Space>
        }
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmitFavorite}
        confirmLoading={submitting}
        width={600}
        okText="确认收藏"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item
            label="话术内容"
            name="content"
            rules={[{ required: true, message: '请输入话术内容' }]}
          >
            <TextArea rows={4} placeholder="请输入完整话术文本" maxLength={1000} showCount />
          </Form.Item>

          <Form.Item
            label="适用场景"
            name="scenario"
            rules={[{ required: true, message: '请输入适用场景' }]}
          >
            <Input placeholder="如：售后咨询、退货安抚" maxLength={100} />
          </Form.Item>

          <Form.Item label="来源话术ID (可选)" name="sourceTalkId">
            <Input placeholder="如AI工作台生成的话术ID" maxLength={128} />
          </Form.Item>

          <Form.Item label="生成时间 (可选)" name="generatedAt">
            <Input type="datetime-local" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="标签" required>
            <Space.Compact style={{ width: '100%' }}>
              <Input
                placeholder="输入标签，最多5个字"
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onPressEnter={handleAddTag}
                maxLength={5}
              />
              <Button type="primary" onClick={handleAddTag}>
                添加
              </Button>
            </Space.Compact>
            <div style={{ marginTop: 8 }}>
              {tempTags.map((tag) => (
                <Tag key={tag} closable onClose={() => handleRemoveTag(tag)} color="cyan">
                  {tag}
                </Tag>
              ))}
              {tempTags.length === 0 && (
                <Text type="secondary" style={{ fontSize: 12 }}>
                  至少添加1个标签（≤5字符）
                </Text>
              )}
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AiCustomer;