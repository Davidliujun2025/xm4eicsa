import React, { useEffect, useState } from 'react';
import {
  Button,
  Card,
  Empty,
  Form,
  Input,
  List,
  Modal,
  Pagination,
  Select,
  Space,
  Spin,
  Tag,
  Tooltip,
  Typography,
  message,
} from 'antd';
import {
  CheckCircleOutlined,
  DeleteOutlined,
  PlusOutlined,
  SearchOutlined,
  StarFilled,
} from '@ant-design/icons';
import { deleteFavorite, listTags, markUsed, searchFavorites, toggleFavorite } from '../../api/scriptFavorite';
import type { FavoriteResponse, ToggleFavoriteRequest } from '../../types/favorite';

const { TextArea } = Input;
const { Text } = Typography;

const mockFavorites: FavoriteResponse[] = [
  {
    id: 'demo-1',
    sourceTalkId: 'ai-talk-10001',
    content: '您好，感谢您的耐心等待。关于退换货问题，我们会优先核实订单状态，并根据平台规则为您提供处理方案。',
    scenario: '售后安抚',
    generatedAt: '2026-07-22T10:20:30Z',
    tags: ['售后', '退货'],
    createdAt: '2026-07-22T10:21:00Z',
    lastUsedAt: '2026-07-26T09:12:00Z',
  },
  {
    id: 'demo-2',
    sourceTalkId: 'ai-talk-10002',
    content: '亲，商品发出后会第一时间同步物流单号，您可以在订单详情中查看最新运输进度。',
    scenario: '物流咨询',
    generatedAt: '2026-07-23T14:05:00Z',
    tags: ['物流', '售前'],
    createdAt: '2026-07-23T14:06:00Z',
    lastUsedAt: '2026-07-25T18:30:00Z',
  },
  {
    id: 'demo-3',
    sourceTalkId: 'ai-talk-10003',
    content: '这款商品目前支持官方活动优惠，具体可领取的券和最终价格请以提交订单页展示为准。',
    scenario: '优惠咨询',
    generatedAt: '2026-07-24T11:15:00Z',
    tags: ['优惠', '活动'],
    createdAt: '2026-07-24T11:16:00Z',
    lastUsedAt: '2026-07-24T16:45:00Z',
  },
];

const AiCustomer: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [list, setList] = useState<FavoriteResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [selectedTag, setSelectedTag] = useState<string>();
  const [tagOptions, setTagOptions] = useState<string[]>([]);
  const [usingMock, setUsingMock] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [tagInput, setTagInput] = useState('');
  const [tempTags, setTempTags] = useState<string[]>([]);
  const [form] = Form.useForm();
  const size = 12;

  const applyMockData = () => {
    const normalizedKeyword = keyword.trim();
    const filtered = mockFavorites.filter((item) => {
      const matchesKeyword = !normalizedKeyword || item.content.includes(normalizedKeyword) || item.tags.some((tag) => tag.includes(normalizedKeyword));
      const matchesTag = !selectedTag || item.tags.includes(selectedTag);
      return matchesKeyword && matchesTag;
    });
    setList(filtered);
    setTotal(filtered.length);
    setTagOptions(Array.from(new Set(mockFavorites.flatMap((item) => item.tags))));
    setUsingMock(true);
  };

  const fetchList = async () => {
    setLoading(true);
    try {
      const res = await searchFavorites(keyword || undefined, selectedTag, page, size);
      setList(res.items);
      setTotal(res.total);
      setUsingMock(false);
    } catch {
      applyMockData();
    } finally {
      setLoading(false);
    }
  };

  const fetchTags = async () => {
    try {
      const res = await listTags();
      setTagOptions(res);
    } catch {
      setTagOptions(Array.from(new Set(mockFavorites.flatMap((item) => item.tags))));
    }
  };

  useEffect(() => {
    fetchList();
  }, [page, selectedTag]);

  useEffect(() => {
    fetchTags();
  }, []);

  const handleSearch = () => {
    setPage(0);
    fetchList();
  };

  const handleUse = async (id: string) => {
    if (usingMock) {
      message.success('本地预览：已标记使用');
      return;
    }
    await markUsed(id);
    message.success('已标记使用');
    fetchList();
  };

  const handleDelete = (id: string, content: string) => {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除话术：「${content.length > 20 ? `${content.slice(0, 20)}...` : content}」吗？`,
      okText: '确认删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        if (usingMock) {
          message.success('本地预览：已删除');
          return;
        }
        await deleteFavorite(id);
        message.success('删除成功');
        fetchList();
      },
    });
  };

  const openModal = () => {
    form.resetFields();
    setTempTags([]);
    setTagInput('');
    setModalVisible(true);
  };

  const handleAddTag = () => {
    const value = tagInput.trim();
    if (!value) {
      message.warning('请输入标签内容');
      return;
    }
    if (value.length > 5) {
      message.warning('标签最多5个字符');
      return;
    }
    if (tempTags.includes(value)) {
      message.warning('标签已存在');
      return;
    }
    setTempTags([...tempTags, value]);
    setTagInput('');
  };

  const handleSubmitFavorite = async () => {
    const values = await form.validateFields();
    if (tempTags.length === 0) {
      message.warning('请至少添加1个标签');
      return;
    }
    setSubmitting(true);
    try {
      const payload: ToggleFavoriteRequest = {
        content: values.content.trim(),
        scenario: values.scenario.trim(),
        tags: tempTags,
        sourceTalkId: values.sourceTalkId || undefined,
        generatedAt: values.generatedAt ? new Date(values.generatedAt).toISOString() : undefined,
      };
      if (usingMock) {
        message.success('本地预览：收藏成功');
        setModalVisible(false);
        return;
      }
      const res = await toggleFavorite(payload);
      message.success(res.message || '收藏成功');
      setModalVisible(false);
      fetchList();
      fetchTags();
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div style={{ padding: 24, background: '#f5f7fa', minHeight: 'calc(100vh - 56px)' }}>
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ margin: 0, fontSize: 22, fontWeight: 600 }}>个人话术库</h2>
        <Text type="secondary">管理收藏的 AI 话术，快速复用提升效率</Text>
        {usingMock && <Tag color="gold" style={{ marginLeft: 12 }}>本地模拟数据</Tag>}
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap>
            <Input
              placeholder="搜索话术内容或标签"
              prefix={<SearchOutlined />}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onPressEnter={handleSearch}
              style={{ width: 260 }}
              allowClear
            />
            <Select
              placeholder="筛选标签"
              allowClear
              value={selectedTag}
              onChange={(value) => setSelectedTag(value || undefined)}
              style={{ width: 150 }}
              options={tagOptions.map((tag) => ({ label: tag, value: tag }))}
            />
            <Button type="primary" onClick={handleSearch}>搜索</Button>
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openModal}>收藏话术</Button>
        </Space>
      </Card>

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
                    <Tooltip title="标记使用（置顶排序）" key="use">
                      <Button type="text" icon={<CheckCircleOutlined />} onClick={() => handleUse(item.id)} style={{ color: '#52c41a' }}>使用</Button>
                    </Tooltip>,
                    <Tooltip title="取消收藏" key="delete">
                      <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDelete(item.id, item.content)}>删除</Button>
                    </Tooltip>,
                  ]}
                >
                  <div style={{ marginBottom: 12, flex: 1 }}>
                    <Text ellipsis={{ tooltip: item.content }} style={{ fontSize: 14 }}>{item.content}</Text>
                    <div style={{ marginTop: 10 }}><Tag color="blue">{item.scenario}</Tag></div>
                    <div style={{ marginTop: 6 }}>
                      {item.tags.map((tag) => <Tag key={tag} color="cyan" style={{ marginBottom: 4 }}>{tag}</Tag>)}
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
          <Pagination current={page + 1} pageSize={size} total={total} onChange={(nextPage) => setPage(nextPage - 1)} showSizeChanger={false} showQuickJumper />
        </div>
      )}

      <Modal
        title={<Space><StarFilled style={{ color: '#faad14' }} /><span>收藏话术</span></Space>}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={handleSubmitFavorite}
        confirmLoading={submitting}
        width={600}
        okText="确认收藏"
        cancelText="取消"
      >
        <Form form={form} layout="vertical" autoComplete="off">
          <Form.Item label="话术内容" name="content" rules={[{ required: true, message: '请输入话术内容' }]}>
            <TextArea rows={4} placeholder="请输入完整话术文本" maxLength={1000} showCount />
          </Form.Item>
          <Form.Item label="适用场景" name="scenario" rules={[{ required: true, message: '请输入适用场景' }]}>
            <Input placeholder="如：售后咨询、退货安抚" maxLength={100} />
          </Form.Item>
          <Form.Item label="来源话术ID（可选）" name="sourceTalkId">
            <Input placeholder="如 AI 工作台生成的话术 ID" maxLength={128} />
          </Form.Item>
          <Form.Item label="生成时间（可选）" name="generatedAt">
            <Input type="datetime-local" style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="标签" required>
            <Space.Compact style={{ width: '100%' }}>
              <Input placeholder="输入标签，最多5个字" value={tagInput} onChange={(event) => setTagInput(event.target.value)} onPressEnter={handleAddTag} maxLength={5} />
              <Button type="primary" onClick={handleAddTag}>添加</Button>
            </Space.Compact>
            <div style={{ marginTop: 8 }}>
              {tempTags.map((tag) => <Tag key={tag} closable onClose={() => setTempTags(tempTags.filter((item) => item !== tag))} color="cyan">{tag}</Tag>)}
              {tempTags.length === 0 && <Text type="secondary" style={{ fontSize: 12 }}>至少添加1个标签（≤5字符）</Text>}
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AiCustomer;