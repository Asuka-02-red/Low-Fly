# @company/ui-ux-pro

基于 `ui-ux-pro-max-skill` 设计规范的全新自研组件库。

## 技术栈
- React / Styled Components
- TypeScript
- React Testing Library (单测)
- Chromatic (视觉回归)

## 目录结构
- `/components` - 核心组件
- `/theme` - 设计令牌 (Design Tokens) 与 CSS 变量
- `/__tests__` - 单元测试

## 迁移指南 (Migration Guide)
我们提供了一个 Codemod 脚本用于将现有的 `Element Plus` 代码自动化替换为 `@company/ui-ux-pro` 组件。

**运行脚本**:
```bash
npx jscodeshift -t scripts/codemod.js src/views/**/*.vue
```

## 变更日志 (CHANGELOG)
### v1.0.0-alpha.1
- ✨ 新增 `Button` 组件，支持 3 种 variant 与 size。
- ✨ 新增 `Grid` 布局系统 (12 列 Bootstrap 栅格规范)。
- 🎨 建立 `/theme/tokens.css` 全局设计令牌。
