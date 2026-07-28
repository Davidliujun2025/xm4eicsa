# Tencent Cloud Ubuntu Runbook (42.193.201.236)

This runbook applies the non-conflicting backend port plan, configures Nginx reverse proxy, starts services in background, and validates xm4 + end-to-end flows.

## 1) Backend Port Plan

- login: `8080`
- forgot-password: `8082`
- create-agent-account: `8083`
- forbidden-words-management: `8084`
- ai-chat-workbench: `8085`

## 2) Build JARs

```bash
cd /path/to/xm4eicsa

mvn -f features/login/server/pom.xml -DskipTests package
mvn -f features/forgot-password/server/pom.xml -DskipTests package
mvn -f features/create-agent-account/server/pom.xml -DskipTests package
mvn -f features/forbidden-words-management/server/pom.xml -DskipTests package
mvn -f features/ai-chat-workbench/server/pom.xml -DskipTests package
```

## 2.1) Build And Upload Latest Frontend UI

From local workspace:

```bash
cd /path/to/xm4eicsa

# Optional overrides:
# export TENCENT_CLOUD_HOST=42.193.201.236
# export TENCENT_CLOUD_SSH_USER=ubuntu
# export TENCENT_CLOUD_SSH_PORT=22
# export TENCENT_WEB_ROOT=/var/www/xm4eicsa

./scripts/deploy-frontends-tencent.sh
```

This script builds and uploads the latest UI for:

- `/` (login)
- `/forgot-password/`
- `/admin/users/`
- `/admin/tokens/`
- `/admin/forbidden-words/`
- `/workbench/`
- `/token-usage/`

## 3) Create Runtime Env File

Create `/etc/xm4eicsa/xm4.env`:

```bash
sudo mkdir -p /etc/xm4eicsa
sudo tee /etc/xm4eicsa/xm4.env >/dev/null <<'EOF'
XM4_MYSQL_URL=jdbc:mysql://127.0.0.1:3306/xm4?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
XM4_MYSQL_USERNAME=root
XM4_MYSQL_PASSWORD=YOUR_REAL_PASSWORD
JAVA_OPTS=-Xms256m -Xmx512m
EOF
```

## 4) Start All Services In Background

```bash
cd /path/to/xm4eicsa
source /etc/xm4eicsa/xm4.env
mkdir -p logs

nohup env SERVER_PORT=8080 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java $JAVA_OPTS -jar features/login/server/target/ai-customer-service-login-0.0.1-SNAPSHOT.jar \
  > logs/login.out 2>&1 &

nohup env SERVER_PORT=8082 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java $JAVA_OPTS -jar features/forgot-password/server/target/password-reset-backend-0.1.0-SNAPSHOT.jar \
  > logs/forgot-password.out 2>&1 &

nohup env SERVER_PORT=8083 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java $JAVA_OPTS -jar features/create-agent-account/server/target/create-agent-account-server-0.0.1-SNAPSHOT.jar \
  > logs/create-agent-account.out 2>&1 &

nohup env FORBIDDEN_WORDS_SERVER_PORT=8084 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java $JAVA_OPTS -jar features/forbidden-words-management/server/target/forbidden-words-server-0.0.1-SNAPSHOT.jar \
  > logs/forbidden-words.out 2>&1 &

nohup env SERVER_PORT=8085 XM4_MYSQL_URL="$XM4_MYSQL_URL" XM4_MYSQL_USERNAME="$XM4_MYSQL_USERNAME" XM4_MYSQL_PASSWORD="$XM4_MYSQL_PASSWORD" \
  java $JAVA_OPTS -jar features/ai-chat-workbench/server/target/ai-chat-workbench-server-0.0.1-SNAPSHOT.jar \
  > logs/ai-chat-workbench.out 2>&1 &
```

Check listeners:

```bash
ss -lntp | grep -E ':8080|:8082|:8083|:8084|:8085'
```

## 5) Apply Nginx Config

```bash
sudo cp deploy/nginx/xm4eicsa.conf.example /etc/nginx/conf.d/xm4eicsa.conf
sudo nginx -t
sudo systemctl reload nginx
```

Note:
- update TLS cert paths in config first
- static root should point to `/var/www/xm4eicsa`

## 6) Validation Commands

### 6.1 API connectivity

```bash
curl -k -i https://42.193.201.236/api/v1/public/login-config
curl -k -i https://42.193.201.236/api/v1/auth/me
curl -k -i https://42.193.201.236/api/admin/customer-service-users/statistics
curl -k -i 'https://42.193.201.236/api/forbidden-words?platform=ALL&page=1&size=1'
curl -k -i 'https://42.193.201.236/api/chat-audit/logs?page=1&size=1'
```

Expected:
- no `502 Bad Gateway`
- normal business response (`200`/`401`/`403` are all acceptable as long as not `502`)

### 6.2 xm4 database access

```bash
mysql -uroot -p -e "SHOW DATABASES LIKE 'xm4';"
mysql -uroot -p -e "USE xm4; SHOW TABLES;"
```

Optional backend DB proof from logs:

```bash
grep -Ei 'exception|error|refused|flyway|datasource|jdbc' logs/*.out | tail -n 80
```

### 6.3 SPA static resources (fix login-bg.png returning HTML)

```bash
curl -k -I https://42.193.201.236/login-bg.png
curl -k -s https://42.193.201.236/login-bg.png | head
```

Expected:
- image request should be `200 image/*` or `404` if file is missing
- should NOT be `200 text/html` for missing image

### 6.4 UI flow check

1. Open `https://42.193.201.236/`
2. Click "忘记密码" and verify redirect to `/forgot-password/`
3. Click "返回登录" and verify back to `/`
4. Login with a test user and verify post-login navigation
5. From customer/admin sidebars, check cross-page navigation still works

## 7) Troubleshooting

- `502 Bad Gateway`:
  - backend not started
  - wrong `proxy_pass` port
  - service started but crashed after startup (`logs/*.out`)
- `ERR_CERT_*`:
  - certificate chain or key mismatch
- database errors:
  - wrong `XM4_MYSQL_URL`/username/password
  - MySQL not listening on `127.0.0.1:3306`
