# 腾讯云 Ubuntu 单体部署

## 1. 构建

```bash
cd /path/to/xm4eicsa
mvn clean package
./scripts/deploy-frontends-tencent.sh build-only
```

最终后端产物只有一个：

```text
application/target/xm4eicsa-application-1.0.0-SNAPSHOT.jar
```

## 2. 环境变量

创建 `/etc/xm4eicsa/xm4.env`：

```bash
XM4_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
XM4_MYSQL_USERNAME=root
XM4_MYSQL_PASSWORD=replace-me
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
JWT_SECRET_BASE64=replace-with-a-random-base64-secret
BOOTSTRAP_ADMIN_ENABLED=false
JAVA_OPTS=-Xms256m -Xmx512m
```

## 3. 启动

```bash
./scripts/start-backends-ubuntu.sh
tail -f logs/xm4eicsa.out
```

只应看到 `8080` 一个后端监听端口。首次启动会执行
`application/src/main/resources/db/xm4` 下的 Flyway 迁移；Hibernate 只做结构校验。

## 4. Nginx 与前端

```bash
./scripts/deploy-frontends-tencent.sh
sudo cp deploy/nginx/xm4eicsa.conf.example /etc/nginx/conf.d/xm4eicsa.conf
sudo nginx -t
sudo systemctl reload nginx
```

Nginx 将所有 `/api/` 请求转发到 `127.0.0.1:8080`。

## 5. 验证

```bash
./scripts/verify-ubuntu-deploy.sh https://your-domain.example
```

`200`、`401` 或 `403` 都表示后端链路已到达；`502` 表示单体应用未启动或代理配置错误。
