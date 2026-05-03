# LinkFlowServices 项目代码规范

本文档用于统一 `LinkFlowServices` 后端代码风格、接口契约和基础工程规则。项目技术栈为 Java 17、Spring Boot、Spring MVC、JPA。

## 命名约定

- 类、接口、枚举、record：`PascalCase`
- 方法、字段、局部变量：`camelCase`
- 常量：`UPPER_SNAKE_CASE`
- 包名：全小写，按业务域分层
- JSON 字段、URL 参数、错误 details 字段：`snake_case`
- 数据库表名、列名：`snake_case`

说明：

- Java 代码内部不使用 `snake_case` 方法名，否则会偏离 Java 主流惯例并增加维护成本。
- 对外 API 契约使用 `snake_case`，通过 `@JsonProperty`、`@JsonGetter` 等方式与 Java 内部命名解耦。

## 注释要求

- 公开类、公开接口、公开 record、公开方法优先使用 Javadoc。
- Javadoc 用来说明业务意图、边界、异常、兼容原因或非显而易见的设计取舍。
- 不要用注释重复描述代码已经清楚表达的内容。
- 复杂逻辑可以添加少量行内注释，说明“为什么这样处理”。
- 私有简单方法不强制写注释，避免注释噪音。
- 注释语言和文件编码保持一致，避免乱码。

## 错误处理

- 使用自定义业务异常，例如 `ApiException`。
- Controller 不为每个接口手写重复 `try/catch`。
- API 异常统一交给 `ApiExceptionHandler` 转换为标准错误响应。
- 只有在需要翻译底层异常、补充上下文、记录关键日志或执行降级逻辑时，才显式 `catch`。
- 不吞异常；捕获后必须重新抛出、转换为业务异常，或返回明确的降级结果。
- Bean Validation 能表达的请求校验优先使用注解，不手写重复判断。

## 日志

- 使用 Spring Boot 默认日志体系：`slf4j` + Logback。
- 日志必须能够关联 `request_id`。
- 优先通过 `RequestIdFilter`、MDC 或统一拦截器注入请求 ID。
- 关键业务日志包含业务对象标识，例如 `link_id`、`back_half`、`user_id`。
- 错误日志包含失败原因和异常类型。
- 避免只打印自由文本；优先保留可检索的结构化字段。

## Controller 约定

- Controller 只负责路由、参数校验、响应封装。
- 业务规则放在 Service 层，不在 Controller 中堆叠流程分支。
- JSON 响应优先使用统一 `ApiResponse` envelope。
- 非 JSON 响应，例如图片流、302 重定向，可以不使用 `ApiResponse` envelope。
- 管理接口使用稳定公开 ID，例如 `link_id`。
- 跳转接口使用短链路由 token，例如 `back_half`。

## Service 约定

- 一个 service 优先聚焦一个明确职责。
- 先选择直接实现，避免为了抽象而抽象。
- 私有辅助方法只保留真正复用或能明显降低认知负担的部分。
- 事务边界放在 service 层。
- 同一业务规则只保留一个权威实现，避免新旧 service 分别维护同一套生成或校验逻辑。

## 短链领域命名

- `short_link`：完整短链，例如 `https://lfow.io/abc1234`。
- `back_half`：短链域名后的可见路径片段，例如 `abc1234`。
- `custom_back_half`：用户自定义的 `back_half`。
- `long_url`：原始长链接。
- `link_id`：管理 API 使用的稳定公开 UUID，不作为跳转 token。

说明：

- 新 API 不再引入 `alias` 作为短链跳转 token 的核心概念。
- `slug` 仅允许作为历史兼容字段或数据库旧列名暂存；新接口和文档优先使用 `back_half`。

## 测试要求

- Controller 改动至少补充或保持对应的 `MockMvc` 测试。
- Service 改动至少覆盖主要成功路径和失败路径。
- 修复 bug 时优先补回归测试，再改实现。
- 兼容字段保留时，需要测试新字段和旧字段的关键行为。

## 当前项目落地结论

已基本符合：

- 类命名与常量命名整体符合 Java 惯例。
- 已有统一异常模型：`ApiException` + `ApiExceptionHandler`。
- 已有请求 ID 透传：`RequestIdFilter` + `ApiResponse`。

仍需持续改进：

- 日志尚未系统性携带 `request_id`。
- 部分控制器存在可下沉到 Service 层的逻辑。
- 部分历史字段仍使用 `slug`，需要逐步迁移到 `back_half` 语义。
