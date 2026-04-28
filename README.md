# 低空驿站一站式数字化服务平台

## 项目概览

本仓库包含 Android 客户端、Spring Boot 后端、Vue 3 管理端三端工程，用于展示低空经济场景下的任务、订单、合规、培训、消息与平台治理闭环。

## 仓库结构

```text
project code/
├─ app/          Android 主应用，当前主要业务页面实现位于此目录
├─ server/       Spring Boot 后端服务
├─ admin-web/    Vue 3 + TypeScript 管理端
├─ core/         Android 公共能力模块
├─ feature/      Android 独立特性模块（当前保留 risk）
├─ docs/         项目说明文档
└─ .github/      CI / Deploy / Release 流水线
```

## 当前重点能力

1. Android 端支持飞手、企业、机构、管理员四类角色差异化展示。
2. 企业注册流程包含材料上传校验，任务、订单、合规、培训、消息与个人中心能力已打通。
3. 后端提供鉴权、业务编排、持久化与管理端聚合接口。
4. 管理端支持概览、用户、项目、订单、分析、设置、日志与工单治理。
5. 仓库保留 CI 流水线与多端源码，适合比赛演示、答辩和后续裁剪迭代。

## 说明书入口

- 项目说明书见 [docs/项目说明书.md](./docs/项目说明书.md)
- 版本变更记录见 [CHANGELOG.md](./CHANGELOG.md)
