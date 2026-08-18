# 生产部署

## 目录

- `eduai.service` — systemd 服务单元
- `eduai.env.example` — 环境变量模板（复制后填真实值）

## 部署步骤

```bash
# 1. 构建可执行 jar
mvn -pl eduai-system -am clean package -DskipTests
# 产物：eduai-system/target/eduai-system-1.0.0-SNAPSHOT.jar

# 2. 上传到服务器并放置
sudo mkdir -p /opt/eduai/{logs,uploads}
sudo cp eduai-system-1.0.0-SNAPSHOT.jar /opt/eduai/

# 3. 准备环境变量（真实凭据，权限 600）
sudo mkdir -p /etc/eduai
sudo cp eduai.env.example /etc/eduai/eduai.env
sudo vim /etc/eduai/eduai.env     # 填真实 DB/Redis/AI 值
sudo chmod 600 /etc/eduai/eduai.env

# 4. 创建运行用户
sudo useradd -r -s /sbin/nologin eduai
sudo chown -R eduai:eduai /opt/eduai

# 5. 安装并启动服务
sudo cp eduai.service /etc/systemd/system/eduai.service
sudo systemctl daemon-reload
sudo systemctl enable --now eduai
sudo systemctl status eduai     # 确认 running
journalctl -u eduai -f          # 实时日志
```

## 运维常用

```bash
sudo systemctl restart eduai    # 重启
sudo systemctl stop eduai       # 停止
journalctl -u eduai -n 100      # 最近 100 行日志
```

## 说明

- 所有凭据走环境变量注入（见 `eduai.env.example`），代码与仓库不含生产密码。
- 生产 `spring.jpa.hibernate.ddl-auto=validate`：只校验表结构、不自动改表；表结构变更需手工跑 `docs/sql/` 迁移脚本。
- 云 Redis 有密码时填 `REDIS_PASSWORD`，无密码则留空（后端自动跳过 AUTH）。
- 上传文件落盘路径由 `UPLOAD_DIR` 决定，确保该目录可写、且被 Nginx 反代 `/uploads/`。
