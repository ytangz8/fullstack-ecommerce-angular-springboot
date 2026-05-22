# 微服务重构架构规划

> 当前单体: `com.blue.ecommerce` (Spring Boot 3.4.4, Java 17, H2/MySQL, Spring Data REST, Spring Security, WebFlux, Stripe SDK)
> 目标: 按领域边界拆分为多个独立部署、独立数据库的微服务,并以渐进式 (Strangler Fig) 路径迁移。

---

## 1. 单体现状梳理

### 1.1 实体与关系 (`com.blue.ecommerce.entity`)

| 实体 | 关键关系 | 备注 |
|------|---------|------|
| `Product` | `@ManyToOne ProductCategory` | sku/name/price/stock,只读对外暴露 |
| `ProductCategory` | `@OneToMany Product` (cascade ALL) | |
| `Customer` | `@OneToMany Order` (cascade ALL) | 邮箱作为业务键 (`findByEmail`) |
| `Order` | `@ManyToOne Customer`、`@OneToMany OrderItem`、`@OneToOne` shipping/billing `Address` (cascade ALL) | 由 Customer 级联保存 |
| `OrderItem` | `@ManyToOne Order`,**`productId` 是裸 `Long` (无 JPA 关联)** | ⭐ 天然的服务拆分缝隙 |
| `Address` | `@OneToOne Order` (反向) | 地址生命周期与订单绑定 |
| `Country` | `@OneToMany State` | 静态参考数据 |
| `State` | `@ManyToOne Country` | 按 country code 查询 |

**关键观察**

- `OrderItem.productId` 已经只是 `Long`,没有 JPA `@ManyToOne` 到 `Product` — 这是订单上下文和商品上下文最自然的拆分点,不需要打破现有外键。
- `Customer ↔ Order` 走的是 `CascadeType.ALL`,`ChekcoutServiceImpl.placeOrder` 在一个 `@Transactional` 中通过 `customerRepository.save(customer)` 级联保存订单、订单项和地址。这是拆分时**最大的改造点**。
- `Address` 与 `Order` 一一绑定且 cascade,地址在领域上属于订单聚合的内部细节,适合留在 Order 服务内,不另立服务。
- `Country/State` 是纯静态参考数据,与业务事务无任何耦合,适合独立但很轻量。

### 1.2 仓储层 (`dao`)

| Repository | 对外暴露 (Spring Data REST) | 自定义查询 |
|-----------|--------------------------|-----------|
| `ProductRepository` | `@RepositoryRestResource`,GET 公开,写方法在 `MyDataRestConfig` 禁用 | `findByCategoryId`, `findByNameContaining` |
| `ProductCategoryRepository` | `@RepositoryRestResource(path=product-category)`, 只读 | — |
| `CountryRepository` | `@RepositoryRestResource(path=countries)`, 只读 | — |
| `StateRepository` | `@RepositoryRestResource`, 只读 | `findByCountryCode` |
| `OrderRepository` | `@RepositoryRestResource`, 只读 | `findByCustomerEmailOrderByDateCreatedDesc` (订单历史页) |
| `CustomerRepository` | **不暴露** (无 `@RepositoryRestResource`) | `findByEmail` |

所有公开端点都被 `MyDataRestConfig` 禁用了 `POST/PUT/PATCH/DELETE` — 写路径只能通过 `CheckoutController` 进入。

### 1.3 服务层 (`service`)

`ChekcoutServiceImpl.placeOrder(Purchase)` 单事务内完成:

1. 生成 `orderTrackingNumber` (UUID)
2. 把 `Purchase.orderItems` 挂到 `Order` 上
3. 设置 shipping/billing `Address`
4. 通过 email 找 `Customer`,找不到就用 DTO 里的新 `Customer`
5. `customer.add(order)` + `customerRepository.save(customer)` (级联落库)
6. 返回 `PurchaseResponse(trackingNumber)`

`createPaymentIntent(PaymentInfo)` 直接调用 Stripe SDK,`Stripe.apiKey` 在构造器中静态注入。

### 1.4 控制器层 (`controller`)

| Controller | 路径 | 职责 |
|-----------|------|-----|
| `CheckoutController` | `POST /api/checkout/purchase`、`POST /api/checkout/payment-intent` | 下单 + Stripe 支付意图 |
| `ChatController` | `POST /api/chat/stream` (SSE, `text/event-stream`) | 反向代理到本地 Ollama (`http://localhost:11434`),WebFlux `WebClient` 流式返回 |

`ChatController` 用的是 **WebFlux 反应式栈**,其它走 **Spring MVC** — 这两者在同进程并存其实就是 "两个子系统住在同一个 jar 里",拆分阻力很小。

### 1.5 横切关注点

- `SecurityConfiguration`: 当前所有 `/api/**` 全部 `permitAll()`,CSRF 对 h2-console/chat/checkout 关闭;Okta starter 已注释 — 真正的认证授权是未来 TODO。
- `MyAppConfig` + `MyDataRestConfig`: CORS 白名单 `https://localhost:4200`。
- HTTPS: 8443 端口,`luv2code-keystore.p12`。
- 数据库: H2 内存 (`MODE=MySQL`) + `data.sql` 种子;`pom.xml` 也带了 `mysql-connector-j` — 切到 MySQL 几乎是无缝的。

---

## 2. 微服务拆分方案

### 2.1 服务清单

| # | 服务 | 拥有数据 | 主要端点 | 拆分理由 |
|---|------|---------|---------|---------|
| 1 | **catalog-service** | `product`, `product_category` | `GET /api/products`、`GET /api/product-category` | 读多写少;前端浏览/搜索的核心;独立后可加缓存/CDN |
| 2 | **customer-service** | `customer` | `GET/POST /api/customers`、`GET /api/customers/search/findByEmail` | 身份与档案领域;未来接入 Okta/OIDC 的边界 |
| 3 | **order-service** | `orders`, `order_item`, `address` | `POST /api/checkout/purchase`、`GET /api/orders/search/findByCustomerEmail...` | 订单聚合根 (Order + OrderItem + Address);事务边界自然在这里 |
| 4 | **payment-service** | `payment_intent` (新表) | `POST /api/payments/intent`、`POST /api/payments/webhooks/stripe` | 隔离 Stripe Secret Key 爆炸半径;为未来多支付渠道留口 |
| 5 | **geo-service** | `country`, `state` | `GET /api/countries`、`GET /api/states/search/findByCountryCode` | 静态参考数据,零事务耦合;最小成本的"练手"服务 |
| 6 | **chat-service** | 无 (无状态代理) 或将来加 `conversation` | `POST /api/chat/stream` (SSE) | 已经是 WebFlux 反应式栈,与 MVC 混在一个 JVM 里浪费资源;独立后可单独水平扩展 |
| 7 | **api-gateway** | — | 路由、CORS、TLS 终止、(未来) JWT 校验 | Spring Cloud Gateway,统一入口 |

可选基础设施: 服务注册中心 (Eureka/Consul 或 k8s 服务发现)、集中配置 (Spring Cloud Config / ConfigMap)、链路追踪 (OpenTelemetry + Tempo/Jaeger)、消息总线 (Kafka 或 RabbitMQ,用于异步事件)。

### 2.2 拆分前后对比图

```
┌────────────────────────────────────────────┐
│   Monolith (今天)                          │
│   spring-boot-ecommerce (port 8443)        │
│   ├ controller/                            │
│   │   ├ CheckoutController                 │
│   │   └ ChatController (WebFlux)           │
│   ├ service/CheckoutService                │
│   ├ dao/*  (Spring Data REST 暴露)         │
│   ├ entity/* (Product/Customer/Order/…)    │
│   └ H2 (MODE=MySQL, 单库)                   │
└────────────────────────────────────────────┘

                      ▼

┌────────────────────────────────────────────┐
│   Angular SPA (https://localhost:4200)     │
└───────────────────┬────────────────────────┘
                    │ HTTPS
                    ▼
            ┌───────────────────┐
            │   api-gateway     │  ← 路由 / CORS / Auth
            └─┬─┬─┬─┬─┬─┬───────┘
              │ │ │ │ │ │
   ┌──────────┘ │ │ │ │ └──────────┐
   ▼            ▼ │ │ ▼            ▼
┌────────┐ ┌────────┐ ┌─────────┐ ┌──────────┐
│catalog │ │customer│ │ order   │ │ payment  │
│-service│ │-service│ │-service │ │ -service │
└───┬────┘ └───┬────┘ └────┬────┘ └────┬─────┘
    │         │           │            │
    ▼         ▼           ▼            ▼
catalog_db customer_db order_db    payment_db
              ▲           │
              └── REST ───┘ (Order 校验/创建 Customer)
                          │ REST → Catalog (校验 price/stock)
                          │ REST → Payment (创建 PaymentIntent)
                          │ events ↓ (OrderPlaced)
                          ▼
                      [Kafka / RabbitMQ]
                          ▲
                          └── 未来: inventory / notification / analytics

┌────────┐         ┌───────────┐
│ geo    │         │  chat     │ ← 直接走 gateway,
│-service│         │ -service  │   或独立子域;无状态代理 Ollama
└───┬────┘         └───────────┘
    ▼
  geo_db
```

### 2.3 数据库拆分

| 库 | 表 | 由谁拥有 |
|----|----|---------|
| `catalog_db` | `product`, `product_category` | catalog-service |
| `customer_db` | `customer` | customer-service |
| `order_db` | `orders`, `order_item`, `address` | order-service |
| `payment_db` | `payment_intent` (新) | payment-service |
| `geo_db` | `country`, `state` | geo-service |
| chat | 暂无 | chat-service (无状态) |

每个服务独享自己的 schema/实例;不允许跨服务直读对方的表。

---

## 3. 关键拆分挑战

### 3.1 Customer ↔ Order 的级联事务

**当前**: `customerRepository.save(customer)` 一次性级联写入 `customer` + `orders` + `order_item` + `address` 四张表,单 ACID 事务。

**拆分后**: Customer 和 Order 在不同库,无法跨库 ACID。两条路:

**方案 A — 编排式 Saga (推荐)**

```
[gateway] POST /api/checkout/purchase
   ↓
[order-service.placeOrder]
   1. 调 customer-service: GET/POST 确保 customerId 存在 (幂等 by email)
   2. (可选) 调 catalog-service: 校验商品价格、库存
   3. 调 payment-service: 创建 PaymentIntent,拿到 clientSecret
   4. 本地事务: 写 orders + order_item + address (customerId 作为外部引用)
   5. 发 OrderPlaced 事件 (outbox 模式)
   6. 返回 { trackingNumber, paymentClientSecret }

如果第 4 步失败 → 调 payment-service.cancel(intentId) 做补偿
```

**方案 B — 简化版 (PoC / MVP)**

订单服务直接持久化 `customerEmail` + `customerId` 作为冗余字段;不强校验 Customer 存在,事后由消费 `OrderPlaced` 事件的 customer-service 异步建档。一致性更弱,但实现成本最低。

无论哪个方案,**`Customer` 实体和 `Order` 实体在 customer-service 和 order-service 中各持有一份精简定义,不再共享 JPA Entity 类。**

### 3.2 Catalog ↔ Order 的引用

好消息: `OrderItem.productId` 已经是裸 `Long`,**今天就没有数据库外键**。拆分时只需:

- Order 服务在写入 `OrderItem` 时,把当时的 `unitPrice` / `imageUrl` 快照存进 `order_item` 表 (今天的代码已经这样做了 — 字段已经在 `OrderItem` 里)。
- 是否调 catalog-service 做"下单时价格校验"是一个产品决策:
  - 严格模式: order-service 同步 REST 调 catalog-service 校验 `unitPrice` 和 `unitsInStock`
  - 宽松模式: 信任前端传来的价格快照 (即今天的行为),延后到对账/审计环节核对

### 3.3 Spring Data REST 的 HAL 链接

当前 Angular 前端调用形如 `/api/products/search/findByCategoryId?id=1&page=0&size=10` 的 Spring Data REST 端点,返回 HAL `_links`/`_embedded` 结构。拆分后:

- 网关需要把 `/api/products/**`、`/api/product-category/**` 路由到 catalog-service
- 把 `/api/orders/**` 路由到 order-service
- 把 `/api/countries/**`、`/api/states/**` 路由到 geo-service
- HAL 中的 `_links.self.href` 由各服务自己生成,**必须保证生成的是网关对外 URL**,否则前端会拿到内部地址。Spring Data REST 可通过 `spring.data.rest.base-path` + `X-Forwarded-*` header 在网关 + Boot 之间正确还原。

### 3.4 Stripe 密钥隔离

当前 `application.properties` 把 `stripe.key.secret` 注入到主进程 — 全 JVM 任何代码都能读到。拆出 payment-service 后,**只有 payment-service 的容器需要这个密钥**,通过 K8s Secret / Vault 注入,大幅缩小爆炸半径。Order 服务通过 REST 调用 payment-service,自己永远拿不到 Stripe key。

### 3.5 认证/授权

今天 `SecurityConfiguration` 全部 `permitAll`,Okta starter 已注释。拆分后建议:

- **gateway** 负责终端认证 (OIDC / JWT 校验)
- 各下游服务只信任 gateway 传过来的 `X-User-Id` / JWT,做粗粒度鉴权
- customer-service 的 `findByEmail` 必须严格限定为只有"自己或管理员"可调用

---

## 4. 服务间通信契约

### 4.1 同步 (REST,推荐 OpenFeign / WebClient)

| 调用方 → 被调方 | 接口 | 用途 |
|---------------|------|-----|
| gateway → * | (所有公开 REST) | 路由 |
| order-service → customer-service | `GET /internal/customers?email=` `POST /internal/customers` | 下单时确保客户存在 |
| order-service → catalog-service | `GET /internal/products/{id}` (可选) | 价格/库存校验 |
| order-service → payment-service | `POST /internal/payments/intent` | 创建 Stripe PaymentIntent |
| payment-service → Stripe | (Stripe SDK) | 真实支付通道 |
| chat-service → Ollama (`localhost:11434`) | `POST /api/chat` | LLM 推理 |

> 内部接口建议放到 `/internal/**` 前缀,网关不暴露这些路径给公网。

### 4.2 异步 (事件总线,Kafka 或 RabbitMQ)

| 事件 | 生产者 | 消费者 |
|------|-------|-------|
| `CustomerCreated` | customer-service | (future) notification, analytics |
| `OrderPlaced` | order-service | (future) inventory, notification, analytics |
| `PaymentSucceeded` / `PaymentFailed` | payment-service (来自 Stripe webhook) | order-service (更新 `order.status`) |

事件落库用 **Outbox Pattern** 保证"业务写 + 事件发"原子性。

---

## 5. 渐进式迁移路线 (Strangler Fig)

按"风险从低到高、价值从高到低"排序,每一步都可独立上线、独立回滚。

| 阶段 | 抽取目标 | 风险 | 验证手段 |
|------|---------|-----|---------|
| **0** | 引入 api-gateway,前端只调网关;网关 100% 反向代理到现有单体 | 极低 | 端到端冒烟测试,所有 e2e 仍走老路径 |
| **1** | 抽出 **chat-service** (WebFlux,无状态,与主链路无耦合) | 低 | `/api/chat/stream` SSE 端到端可用 |
| **2** | 抽出 **geo-service** (只读静态数据) | 低 | 结账页国家/省份下拉列表正常 |
| **3** | 抽出 **catalog-service** (只读) | 中 | 商品列表/搜索/分类页正常,HAL 链接通过网关 |
| **4** | 抽出 **payment-service** (隔离 Stripe key) | 中 | 支付意图创建链路,Stripe webhook 回调通畅 |
| **5** | 抽出 **customer-service** + **order-service** (打破级联,引入 Saga) | 高 | 下单成功率、订单一致性监控、补偿场景演练 |
| **6** | 单体收尾: 删除已迁出的 `dao`/`entity`/`service`,直到主 jar 为空 | 低 | 单体可下线 |
| **7** | 每个服务从 H2 切到独立 MySQL/Postgres 实例 (或 schema) | 中 | 数据迁移脚本 + 影子读写一段时间 |
| **8** | 引入消息总线、Outbox、事件驱动的派生服务 (inventory / notification / analytics) | 中 | — |

每阶段建议保留"灰度开关":网关上对应路径可以一键切回单体,出问题立刻回退。

---

## 6. 技术选型建议

| 关注点 | 推荐 | 理由 |
|--------|------|------|
| 服务框架 | Spring Boot 3.4.x (沿用) | 与现有代码同栈,迁移成本低 |
| 网关 | Spring Cloud Gateway | 与 Spring 生态契合;反应式;可挂 JWT filter |
| 服务发现 | Kubernetes Service / Consul (二选一,**避免** Eureka 自建) | 生产可运维 |
| 配置中心 | K8s ConfigMap + Secret,或 Spring Cloud Config | Stripe key 务必走 Secret 不入仓 |
| 服务间调用 | OpenFeign (MVC) / WebClient (WebFlux) | 与各服务的栈匹配 |
| 消息总线 | Kafka (高吞吐) 或 RabbitMQ (运维更简单) | 看团队熟悉度 |
| 数据库 | 每服务独立 MySQL (单体已带 mysql-connector-j) | 与现有 SQL 兼容 |
| 可观测性 | Spring Boot Actuator + Micrometer + OpenTelemetry + Prometheus + Grafana + Tempo | 标准组合 |
| 部署 | Docker + Kubernetes (Helm chart per service) | 标准做法 |
| CI | 每服务独立流水线,Maven multi-module 或独立 repo | 视团队规模 |

---

## 7. 仓库结构演进

**当前**

```
fullstack-ecommerce-angular-springboot/
├ backend/   ← 单一 Maven 项目
└ frontend/  ← Angular
```

**目标 (Monorepo + Maven 多模块,或多 repo)**

```
fullstack-ecommerce-angular-springboot/
├ frontend/
├ services/
│   ├ api-gateway/
│   ├ catalog-service/
│   ├ customer-service/
│   ├ order-service/
│   ├ payment-service/
│   ├ geo-service/
│   └ chat-service/
├ libs/
│   └ shared-contracts/        ← DTO / event schemas (Avro/Proto/OpenAPI)
├ deploy/
│   ├ docker-compose.yml       ← 本地一键起所有服务 + Kafka + MySQL
│   └ k8s/                     ← Helm charts
└ ARCHITECTURE.md
```

**注意**: `shared-contracts` 只放 DTO / 事件 schema,**绝不**放 JPA Entity — 否则会变相把数据库 schema 耦合回去,等于没拆。

---

## 8. 风险与开放问题

1. **HAL `_links` 通过网关后的 URL 重写**: 需要 PoC 验证 Spring Data REST + Spring Cloud Gateway 的 `Forwarded` header 协作是否平滑,否则前端 `_links.self.href` 会指向内部 ip。
2. **下单跨服务事务一致性**: 选 Saga 还是"最终一致 + 冗余字段"需要一次架构评审;影响业务上"下单失败但客户已建档"这类边界场景的处理。
3. **认证体系**: 当前是 `permitAll`,生产化必须先把 Okta (或自建 OIDC) 接回来,再做拆分,否则边界一旦扩散就难收口。
4. **数据迁移**: 从单库到多库,种子数据 (`data.sql`) 需要按服务拆分;生产数据如果已有,需要影子库 + 双写方案。
5. **Chat 服务的模型依赖 (`llava:7b`, Ollama on localhost:11434)**: 拆出后 Ollama 是 chat-service 的 sidecar 还是独立部署?GPU 资源调度策略待定。
6. **前端改造范围**: 如果网关前缀保持 `/api/**` 不变,前端理论上零改动;但若分多个域名/前缀,Angular 的 `environment.ts` 需要相应调整。

---

## 9. 下一步建议

1. 评审本文档,确认 7 个服务的边界是否符合产品/团队预期。
2. 起一个 `deploy/docker-compose.yml`,本地把 gateway + 单体 + Kafka + MySQL 起起来,验证阶段 0 (网关反向代理)。
3. 选阶段 1 (chat-service) 作为第一刀,完整跑通"独立部署 + 网关路由 + Angular 调用不变"的闭环,沉淀模板。
4. 把模板复制到 geo-service、catalog-service,完成只读服务的全部抽离。
5. 进入阶段 4-5 前,先把认证体系 (Okta/OIDC) 接回来,确保拆分后边界安全。
