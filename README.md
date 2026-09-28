# 阅微图书在线商城（Bookstore Online Mall）

> 基于 Spring Boot 3 + Vue 3 的前后端分离多角色图书电商平台，包含**用户购物端、商家端、平台管理端**三端完整业务闭环。
>
> 本项目为个人本科毕业设计项目，旨在完整实践 Spring Boot 3 + Vue 3 前后端分离架构，包含完整的电商业务闭环。

---

## 💼 我的核心工作（后端）

- **数据库设计**：负责 20 张核心业务表的结构设计，基于 MySQL + MyBatis-Plus 完成 CRUD 与多表联查。
- **权限与安全**：设计并实现基于 JWT + 双拦截器的分层鉴权体系，完成用户、商家、管理员三类角色的接口隔离与越权防护。
- **缓存与性能**：基于 Redis Hash 实现购物车高并发读写，并引入 SETNX 实现下单防重锁，有效降低数据库压力。
- **并发与业务闭环**：编写原子 SQL（`UPDATE tb_book SET stock = stock - #{num} WHERE id = #{id} AND stock >= #{num}`）防止超卖；结合 `@Scheduled` 定时任务实现超时订单自动取消与库存回补。
- **工程化与规范**：封装全局异常处理器与统一返回格式，规范全站接口响应；使用 Knife4j 生成接口文档。
- **问题排查**：排查并修复订单归属校验、拦截器线程复用导致用户串号等核心 Bug。

---

## 🖼️ 项目截图
<img width="1920" height="912" alt="image" src="https://github.com/user-attachments/assets/51d51515-2a2a-49a5-ae9a-50cc0d058d9d" />
<img width="1920" height="912" alt="image" src="https://github.com/user-attachments/assets/1c4991c9-37f2-4060-a211-71ff6d4068e5" />
<img width="1920" height="912" alt="image" src="https://github.com/user-attachments/assets/60d90c36-2d75-4e15-b235-abc1f7bcb81c" />
<img width="1920" height="912" alt="image" src="https://github.com/user-attachments/assets/e0691222-f124-4d7c-b876-c6caaf774e85" />
<img width="1651" height="895" alt="image" src="https://github.com/user-attachments/assets/4934013a-a6b6-45fb-800e-2daf27f67319" />


## 🛠️ 技术栈

### 后端（bookstore/）
- **核心框架**：JDK 17、Spring Boot 3.3.4、Spring MVC
- **数据访问**：**MyBatis-Plus 3.5.5**（分页插件、逻辑删除、自动填充）、**MySQL 8.x**（20 张表）
- **缓存与中间件**：**Redis**（购物车、下单防重锁、推荐结果缓存、@Cacheable 缓存抽象）
- **安全与鉴权**：**JWT（jjwt 0.11.5）**、spring-security-crypto（BCrypt 加密）
- **工具与文档**：Knife4j 4.5 + SpringDoc OpenAPI 3、Jakarta Validation、Hutool 5.8、Apache POI 5.2、Lombok

### 前端（bookstore-admin/）
- **核心框架**：Vue 3.5（Composition API、`<script setup>`）、Vite 8 构建
- **路由与状态**：Vue Router 5（路由元信息做登录/角色守卫）、Axios（统一请求封装、Token 注入）
- **UI 与可视化**：Element Plus 2.14、ECharts 6 + vue-echarts、element-china-area-data

### 中间件
- MySQL 8.x、Redis（6.x/7.x 均可）

---

## 🔄 核心业务闭环

1. 用户注册登录 → 浏览/搜索/分类/排行/个性化推荐图书；
2. 加入购物车（Redis 存储）→ 提交订单（防重锁 + 原子扣库存）→ **模拟支付**；
3. 商家发货 → 用户确认收货 → 图书评价 / 店铺评价 / 申请售后；
4. 待付款订单超时由定时任务自动取消并回补库存；
5. 用户可提交入驻申请，管理员审核通过后升级为商家；
6. 全流程配套用户—商家、用户—管理员的站内消息会话。

---

## 🚀 核心技术实现与亮点

1. **JWT 双拦截器分层鉴权**
   `JwtInterceptor`（order=1）统一验签，仅放行登录/注册/公开浏览与接口文档；`AuthorizationInterceptor`（order=2）按路由二次判权：`/admin/**` 必须 role=1，`/shop/**` 必须在数据库中拥有归属店铺。

2. **ThreadLocal 用户上下文**
   `UserContext` 用 ThreadLocal 保存当前请求的 userId/role，业务层无需层层传参；请求结束 `afterCompletion` 中强制 `clear()`，防止 Tomcat 线程复用导致用户串号。

3. **Redis Hash 购物车与 SETNX 下单防重锁**
   购物车 key 为 `cart:{userId}`、field 为 bookId，支持数量累加覆盖、单选/全选、批量删除；下单入口执行 `setIfAbsent("order:submit:lock:{userId}", ..., 5s)`，同一用户 5 秒内重复提交直接拒绝。

4. **原子 SQL 防止超卖**
   扣库存使用条件更新，影响行数为 0 即库存不足并回滚整单；取消/退款时对称地原子回补库存、回退销量。

5. **多商家场景的联合唯一索引**
   `uk_isbn_shop(isbn, shop_id)`：同一店铺内 ISBN 唯一，不同商家可售卖同一本 ISBN 的图书，比 ISBN 全局唯一更符合多商家商城模型。

6. **消息会话双向独立软删除与权限校验**
   会话表设 `user1_deleted`、`user2_deleted` 两个独立删除位；订单接口在 Service/Controller 层再做资源归属校验（支付/取消必须是买家本人；商家发货/查看订单必须 shopId 归属本店）。

7. **定时任务自动关闭超时订单**
   `@Scheduled(cron = "0 */5 * * * ?")` 每 5 分钟扫描"待付款且创建超过 30 分钟"的订单，自动取消并回补库存；取消方法内部再次校验订单状态，规避竞态。

8. **全局异常处理器**
   `@RestControllerAdvice` 统一收口业务异常、`@Valid` 参数校验、JSON 解析、参数类型/缺失、404 与未知运行时异常；对外返回固定友好文案，SQL 报错与堆栈只进日志，不泄漏给前端。

---

## 📁 项目目录结构

```text
GraduationProject/
├── bookstore/                     # 后端 Spring Boot 工程
│   └── src/main/java/com/bookstore/
│       ├── controller/
│       │   ├── front/             # 用户端接口
│       │   ├── shop/              # 商家端接口
│       │   ├── admin/             # 管理员端接口
│       │   └── ...                # 通用接口
│       ├── service/               # 业务接口与实现
│       ├── mapper/                # MyBatis-Plus Mapper
│       ├── entity/                # 数据库实体（20 个）
│       ├── dto/  vo/              # 请求 DTO 与响应 VO
│       ├── config/                # 配置类
│       ├── interceptor/           # JwtInterceptor、AuthorizationInterceptor
│       └── util/                  # JWT、ThreadLocal 用户上下文等工具
│
└── bookstore-admin/               # 前端 Vue 3 工程
    └── src/
        ├── views/                 # 页面（front / shop / admin）
        ├── components/            # 公共组件
        ├── router/                # 路由表与登录/角色守卫
        └── utils/                 # axios 封装、鉴权等
```

---

## 💻 本地开发部署步骤

### 环境要求
- JDK 17、Maven 3.6+、Node.js 22.18+、MySQL 8.x、Redis 6.x/7.x

### 1. 初始化数据库
```sql
CREATE DATABASE book_store DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE book_store;
-- 导入建表脚本：bookstore/src/main/resources/sql/book_store.sql
```
*注：脚本共 20 张表、无任何初始数据，启动后请自行注册账号。*

### 2. 启动 Redis
确保本机 Redis 已启动并监听 `6379`。

### 3. 后端配置与启动
```bash
cd bookstore
cp src/main/resources/application.example.yml src/main/resources/application.yml
# 修改 MySQL 用户名/密码、Redis 密码
./mvnw spring-boot:run   # Windows PowerShell: .\mvnw.cmd spring-boot:run
```
启动成功后：后端地址 `http://localhost:8080`，接口文档 `http://localhost:8080/doc.html`

### 4. 前端启动
```bash
cd bookstore-admin
cp .env.example .env
npm install
npm run dev
```
访问地址：`http://localhost:5173`

---

## 🔮 技术演进与反思

1. **支付模块**：当前采用模拟支付（后端状态流转）。未来若需对接真实业务，可引入支付宝/微信支付 SDK 并处理异步回调。
2. **消息通知机制**：当前采用 HTTP 轮询实现未读数更新。考虑到轮询带来的延迟与无效请求，若未来接入高并发即时通讯，计划引入 WebSocket 或 Netty 优化长连接。
3. **并发场景设计**：当前利用 Redis SETNX 与数据库原子 SQL 应对常规并发下的防重与超卖。针对更高并发的场景，后续可考虑引入消息队列（如 RabbitMQ）进行异步削峰，或采用 Redisson 分布式锁。
4. **对象存储**：当前上传文件存储在服务器本地磁盘（`./uploads/`），未来可接入 MinIO 或阿里云 OSS，实现多实例部署时的文件共享。
5. **部署方案**：当前仅提供本地开发配置，后续可增加 Dockerfile 与 CI/CD 脚本，提升生产化部署能力。

