# Bitly-style API 联调与 Swagger 测试指南

## 运行地址

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
- Frontend: `http://127.0.0.1:5173`

## Swagger 测试顺序

1. `POST /api/v1/auth/register`

```json
{
  "email": "alice@example.com",
  "username": "alice",
  "password": "Password123!"
}
```

2. `POST /api/v1/auth/login`

```json
{
  "email": "alice@example.com",
  "password": "Password123!"
}
```

复制响应里的 `data.access_token`，在 Swagger 右上角 `Authorize` 中填写：

```text
Bearer <access_token>
```

3. `POST /api/v1/links`

自动生成 back-half： 

```json
{
  "long_url": "https://example.com/campaign",
  "title": "Example campaign",
  "channel": "swagger"
}
```

自定义 back-half：

```json
{
  "long_url": "https://example.com/campaign",
  "title": "Example campaign",
  "custom_back_half": "spring-sale",
  "channel": "swagger"
}
```

期望响应重点字段：

```json
{
  "data": {
    "link_id": "...",
    "back_half": "spring-sale",
    "short_link": "http://localhost:8080/spring-sale",
    "long_url": "https://example.com/campaign"
  }
}
```

4. `GET /api/v1/links`

推荐查询参数：

```text
page=1
size=20
sort=created_at,desc
```

按 back-half 排序：

```text
sort=back_half,asc
```

5. `GET /api/v1/links/{link_id}`

使用创建响应里的 `link_id`。

6. `PATCH /api/v1/links/{link_id}`

```json
{
  "title": "Updated title",
  "custom_back_half": "spring-sale-2",
  "channel": "email"
}
```

7. Redirect 测试

在浏览器打开：

```text
http://localhost:8080/spring-sale-2
```

兼容入口仍可测试：

```text
http://localhost:8080/r/spring-sale-2
```

## PowerShell 快速测试

```powershell
$base = "http://localhost:8080"

$register = @{
  email = "alice@example.com"
  username = "alice"
  password = "Password123!"
} | ConvertTo-Json
Invoke-RestMethod "$base/api/v1/auth/register" -Method Post -ContentType "application/json" -Body $register

$login = @{
  email = "alice@example.com"
  password = "Password123!"
} | ConvertTo-Json
$session = Invoke-RestMethod "$base/api/v1/auth/login" -Method Post -ContentType "application/json" -Body $login
$headers = @{ Authorization = "Bearer $($session.data.access_token)" }

$create = @{
  long_url = "https://example.com/campaign"
  title = "Example campaign"
  custom_back_half = "spring-sale"
  channel = "powershell"
} | ConvertTo-Json
$link = Invoke-RestMethod "$base/api/v1/links" -Method Post -Headers $headers -ContentType "application/json" -Body $create
$link.data

Invoke-RestMethod "$base/api/v1/links?sort=back_half,asc" -Headers $headers
Invoke-WebRequest "$base/$($link.data.back_half)" -MaximumRedirection 0
```

## 前端联调检查

1. 打开 `http://127.0.0.1:5173`
2. 登录或注册
3. 进入 Short Links 页面
4. 创建链接时填写 `Custom back-half`
5. 确认列表展示 `short_link`
6. 点击 Open，确认跳转到 `long_url`
7. 进入详情页，修改 `Custom back-half`，保存后再次测试跳转
