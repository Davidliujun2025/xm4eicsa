import React, { useCallback, useEffect, useState } from 'react';
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
import {
  deleteFavorite,
  listTags,
  markUsed,
  searchFavorites,
  toggleFavorite,
} from '../../api/scriptFavorite';
import type { FavoriteResponse, ToggleFavoriteRequest } from '../../types/favorite';

const { TextArea } = Input;
const { Text } = Typography;
const PAGE_SIZE = 12;

const AiCustomer: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [items, setItems] = useState<FavoriteResponse[]>([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [keyword, setKeyword] = useState('');
  const [selectedTag, setSelectedTag] = useState<string>();
  const [tagOptions, setTagOptions] = useState<string[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [tagInput, setTagInput] = useState('');
  const [tempTags, setTempTags] = useState<string[]>([]);
  const [form] = Form.useForm();

  const loadList = useCallback(async () => {
    setLoading(true);
    try {
      const result = await searchFavorites(keyword || undefined, selectedTag, page, PAGE_SIZE);
      setItems(result.items);
      setTotal(result.total);
    } catch (error) {
      setItems([]);
      setTotal(0);
      message.error(error instanceof Error ? error.message : '话术加载失败');
    } finally {
      setLoading(false);
    }
  }, [keyword, page, selectedTag]);

  const loadTags = useCallback(async () => {
    try {
      setTagOptions(await listTags());
    } catch (error) {
      setTagOptions([]);
      message.error(error instanceof Error ? error.message : '标签加载失败');
    }
  }, []);

  useEffect(() => {
    void loadList();
  }, [loadList]);

  useEffect(() => {
    void loadTags();
  }, [loadTags]);

  async function handleUse(id: string) {
    try {
      await markUsed(id);
      message.success('已标记使用');
      await loadList();
    } catch (error) {
      message.error(error instanceof Error ? error.message : '操作失败');
    }
  }

  function handleDelete(id: string, content: string) {
    Modal.confirm({
      title: '确认删除',
      content: `确定要删除话术“${content.slice(0, 24)}${content.length > 24 ? '…' : ''}”吗？`,
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        await deleteFavorite(id);
        message.success('删除成功');
        await Promise.all([loadList(), loadTags()]);
      },
    });
  }

  function openModal() {
    form.resetFields();
    setTempTags([]);
    setTagInput('');
    setModalVisible(true);
  }

  function addTag() {
    const value = tagInput.trim();
    if (!value || tempTags.includes(value)) return;
    if (value.length > 20) {
      message.warning('标签最多 20 个字符');
      return;
    }
    setTempTags((current) => [...current, value]);
    setTagInput('');
  }

  async function submitFavorite() {
    const values = await form.validateFields();
    if (tempTags.length === 0) {
      message.warning('请至少添加一个标签');
      return;
    }
    setSubmitting(true);
    try {
      const request: ToggleFavoriteRequest = {
        content: values.content.trim(),
        scenario: values.scenario.trim(),
        tags: tempTags,
        sourceTalkId: values.sourceTalkId?.trim() || undefined,
        generatedAt: values.generatedAt
          ? new Date(values.generatedAt).toISOString()
          : undefined,
      };
      const result = await toggleFavorite(request);
      message.success(result.message || '收藏成功');
      setModalVisible(false);
      await Promise.all([loadList(), loadTags()]);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '收藏失败');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div style={{ padding: 24, background: '#f5f7fa', minHeight: 'calc(100vh - 88px)' }}>
      <div style={{ marginBottom: 24 }}>
        <h2 style={{ margin: 0, fontSize: 22 }}>个人话术库</h2>
        <Text type="secondary">当前列表全部来自已登录客服的真实收藏数据</Text>
      </div>

      <Card style={{ marginBottom: 24 }}>
        <Space wrap style={{ width: '100%', justifyContent: 'space-between' }}>
          <Space wrap>
            <Input
              placeholder="搜索话术内容或标签"
              prefix={<SearchOutlined />}
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
              onPressEnter={() => {
                setPage(0);
                void loadList();
              }}
              style={{ width: 260 }}
              allowClear
            />
            <Select
              placeholder="筛选标签"
              allowClear
              value={selectedTag}
              onChange={(value) => {
                setPage(0);
                setSelectedTag(value || undefined);
              }}
              style={{ width: 150 }}
              options={tagOptions.map((tag) => ({ label: tag, value: tag }))}
            />
          </Space>
          <Button type="primary" icon={<PlusOutlined />} onClick={openModal}>
            收藏话术
          </Button>
        </Space>
      </Card>

      <Spin spinning={loading}>
        {items.length === 0 ? (
          <Empty description="暂无收藏话术" style={{ marginTop: 60 }} />
        ) : (
          <List
            grid={{ gutter: 16, xs: 1, sm: 2, lg: 3, xxl: 4 }}
            dataSource={items}
            renderItem={(item) => (
              <List.Item>
                <Card
                  hoverable
                  actions={[
                    <Button
                      key="use"
                      type="text"
                      icon={<CheckCircleOutlined />}
                      onClick={() => void handleUse(item.id)}
                    >
                      使用
                    </Button>,
                    <Button
                      key="delete"
                      type="text"
                      danger
                      icon={<DeleteOutlined />}
                      onClick={() => handleDelete(item.id, item.content)}
                    >
                      删除
                    </Button>,
                  ]}
                >
                  <Text ellipsis={{ tooltip: item.content }}>{item.content}</Text>
                  <div style={{ marginTop: 12 }}><Tag color="blue">{item.scenario}</Tag></div>
                  <div style={{ marginTop: 6 }}>
                    {item.tags.map((tag) => <Tag key={tag}>{tag}</Tag>)}
                  </div>
                </Card>
              </List.Item>
            )}
          />
        )}
      </Spin>

      {total > 0 && (
        <Pagination
          style={{ marginTop: 24, textAlign: 'right' }}
          current={page + 1}
          pageSize={PAGE_SIZE}
          total={total}
          showSizeChanger={false}
          onChange={(nextPage) => setPage(nextPage - 1)}
        />
      )}

      <Modal
        title={<Space><StarFilled style={{ color: '#faad14' }} />收藏话术</Space>}
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => void submitFavorite()}
        confirmLoading={submitting}
        okText="确认收藏"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item label="话术内容" name="content" rules={[{ required: true, message: '请输入话术内容' }]}>
            <TextArea rows={4} maxLength={1000} showCount />
          </Form.Item>
          <Form.Item label="适用场景" name="scenario" rules={[{ required: true, message: '请输入适用场景' }]}>
            <Input maxLength={100} />
          </Form.Item>
          <Form.Item label="来源话术 ID（可选）" name="sourceTalkId">
            <Input maxLength={128} />
          </Form.Item>
          <Form.Item label="生成时间（可选）" name="generatedAt">
            <Input type="datetime-local" />
          </Form.Item>
          <Form.Item label="标签" required>
            <Space.Compact style={{ width: '100%' }}>
              <Input
                value={tagInput}
                onChange={(event) => setTagInput(event.target.value)}
                onPressEnter={addTag}
              />
              <Button type="primary" onClick={addTag}>添加</Button>
            </Space.Compact>
            <div style={{ marginTop: 8 }}>
              {tempTags.map((tag) => (
                <Tag
                  key={tag}
                  closable
                  onClose={() => setTempTags((current) => current.filter((item) => item !== tag))}
                >
                  {tag}
                </Tag>
              ))}
            </div>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AiCustomer;
