# DatI 扩展模块开发指南（Extension Development Guide）

本文档指导开发者如何为 DatI 平台开发和集成扩展模块（如认证方式扩展、搜索引擎适配器、外部授权等）。

---

## 1. 架构理念

DatI 采用 **微核心 + 插件化（Microkernel & Plugins）** 架构：
- **`dati-core`**：核心业务领域模型、基础接口与 SPI 扩展点契约，零第三方具体凭据/引擎依赖；
- **`dati-ext-*`**：可插拔的扩展实现，依赖 `dati-core` 并实现相应的 SPI 接口；
- **`dati-app`**：最终运行与部署单元，按需组装所需的 core 与扩展模块，打包为 Spring Boot Fat JAR。

**激活机制：类路径即激活（Classpath Activation）**
- 部署时，只要扩展模块的 JAR 位于类路径上，Spring 容器便会自动扫描并注入其 Bean（如 `AuthenticationProvider`）；
- 若不需要某个扩展（例如在纯 SSO 环境下禁用本地账号密码），只需在 `dati-app` 的依赖中移除该模块，对应功能与端点即自动消失，无需修改业务代码。

---

## 2. 现存扩展模块

| 模块目录 | 构件坐标 (ArtifactId) | 职责 | 关键依赖 |
|---|---|---|---|
| `backend/ext/auth-local` | `com.dati:dati-auth-local` | 本地账号密码注册与登录、JWT 签发校验 | `jjwt`, `spring-security-crypto` |
| `backend/ext/auth-apikey` | `com.dati:dati-auth-apikey` | 程序化 M2M API Key (`sk_...`) 拦截鉴权与管理 | 仅依赖 `dati-core` |

---

## 3. 开发新扩展模块规范

以开发一个新的认证模块（例如 `auth-oidc`）为例：

### 3.1 目录与命名规范
- **物理目录**：创建在 `backend/ext/` 目录下，使用简洁短名，如 `backend/ext/auth-oidc/`；
- **构件坐标**：
  - `groupId`: `com.dati`
  - `artifactId`: `dati-auth-oidc`（统一带有 `dati-` 前缀）；
- **包名**：建议使用 `com.dati.auth.oidc` 或统一收敛在 `com.dati.*` 下以享受默认组件扫描。

### 3.2 POM 配置
1. 在根 `pom.xml` 的 `<dependencyManagement>` 中声明新模块版本：
   ```xml
   <dependency>
       <groupId>com.dati</groupId>
       <artifactId>dati-auth-oidc</artifactId>
       <version>${project.version}</version>
   </dependency>
   ```
2. 在 `backend/pom.xml` 的 `<modules>` 列表中注册：
   ```xml
   <module>ext/auth-oidc</module>
   ```
3. 在新模块的 `backend/ext/auth-oidc/pom.xml` 中依赖 `dati-core`：
   ```xml
   <parent>
       <groupId>com.dati</groupId>
       <artifactId>dati-parent</artifactId>
       <version>0.3.5</version>
       <relativePath>../../../pom.xml</relativePath>
   </parent>
   <artifactId>dati-auth-oidc</artifactId>

   <dependencies>
       <dependency>
           <groupId>com.dati</groupId>
           <artifactId>dati-core</artifactId>
       </dependency>
       <!-- 模块特有依赖，如 spring-security-oauth2-client -->
   </dependencies>
   ```

### 3.3 实现 SPI 扩展点
实现 `dati-core` 提供的 SPI 接口，并标注 `@Component`（或 `@Service`）：
```java
package com.dati.auth.oidc;

import com.dati.auth.authentication.AuthenticationProvider;
import com.dati.auth.authentication.User;
import com.dati.auth.server.pojo.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OidcAuthenticationProvider implements AuthenticationProvider {

    @Override
    public boolean canAuthenticate(HttpServletRequest request) {
        // 判断请求头是否携带 OIDC Token
        return false;
    }

    @Override
    public Optional<User> authenticate(HttpServletRequest request) {
        // 验证 OIDC Token 并返回 User
        return Optional.empty();
    }

    @Override
    public String login(LoginRequest request) {
        // 处理 OIDC 登录/交换凭证
        return null;
    }

    @Override
    public boolean supports(String type) {
        return "oidc".equalsIgnoreCase(type);
    }
}
```

### 3.4 测试规范
- 编写单元测试覆盖核心逻辑（如 Token 验证、过期处理、错误分支）；
- 若测试需要 Spring Boot 测试切片（如 `@WebMvcTest` 或 `@DataJpaTest`），在 `src/test/java/com/dati/TestApplication.java` 下配置空 `@SpringBootApplication` 类即可。

### 3.5 组装与交付
在 `backend/app/pom.xml` 中引入新模块依赖：
```xml
<dependency>
    <groupId>com.dati</groupId>
    <artifactId>dati-auth-oidc</artifactId>
</dependency>
```
重新打包即可生成包含该扩展能力的运行时 Fat JAR。
