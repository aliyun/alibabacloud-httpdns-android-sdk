# Alicloud HTTPDNS Android SDK

面向 Android 的 HTTP/HTTPS DNS 解析 SDK，提供鉴权与可选 AES 加密、IPv4/IPv6 双栈解析、缓存与调度、预解析等能力。最低支持 Android API 19（Android 4.4）。

## 功能特性

- 鉴权请求与可选 AES 传输加密
- IPv4/IPv6 双栈解析，支持自动/同时解析
- 内存 + 持久化缓存与 TTL 控制，可选择复用过期 IP
- 预解析、区域路由、网络切换自动刷新
- 可定制日志回调与会话追踪 `sessionId`

## 安装（Gradle）

在项目的 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.aliyun.ams:alicloud-android-httpdns:2.6.7'
}
```

请访问 [Android SDK发布说明](https://help.aliyun.com/document_detail/435251.html) 查看最新版本号。

## 快速开始

### Java

```java
import com.alibaba.sdk.android.httpdns.HttpDns;
import com.alibaba.sdk.android.httpdns.HttpDnsService;
import com.alibaba.sdk.android.httpdns.InitConfig;
import com.alibaba.sdk.android.httpdns.RequestIpType;

// 初始化配置
new InitConfig.Builder()
    .setContext(context)
    .setSecretKey("YOUR_SECRET_KEY")
    .setEnableExpiredIp(true)  // 允许返回过期 IP
    .buildFor("YOUR_ACCOUNT_ID");

// 获取实例
HttpDnsService httpDns = HttpDns.getService("YOUR_ACCOUNT_ID");

// 预解析热点域名
httpDns.setPreResolveHosts(new ArrayList<>(Arrays.asList("www.aliyun.com")));

// 解析域名
HTTPDNSResult result = httpDns.getHttpDnsResultForHostSyncNonBlocking("www.aliyun.com", RequestIpType.auto);
String[] ips = result.getIps();
```

### Kotlin

```kotlin
import com.alibaba.sdk.android.httpdns.HttpDns
import com.alibaba.sdk.android.httpdns.InitConfig
import com.alibaba.sdk.android.httpdns.RequestIpType

// 初始化配置
InitConfig.Builder()
    .setContext(context)
    .setSecretKey("YOUR_SECRET_KEY")
    .setEnableExpiredIp(true)  // 允许返回过期 IP
    .buildFor("YOUR_ACCOUNT_ID")

// 获取实例
val httpDns = HttpDns.getService("YOUR_ACCOUNT_ID")

// 预解析热点域名
httpDns.setPreResolveHosts(arrayListOf("www.aliyun.com"))

// 解析域名
val result = httpDns.getHttpDnsResultForHostSyncNonBlocking("www.aliyun.com", RequestIpType.auto)
val ips = result.ips
```

### 提示

- 启动时通过 `setPreResolveHosts()` 预热热点域名
- 如需在刷新期间容忍 TTL 过期，可开启 `setEnableExpiredIp(true)`
- 使用 `getSessionId()` 并与选用 IP 一同记录，便于排障

## 源码构建

```bash
./gradlew clean :httpdns-sdk:assembleRelease
```

构建产物位于 `httpdns-sdk/build/outputs/aar/` 目录。

### 版本说明

项目使用 `productFlavors` 区分不同版本：
- `normal`：中国大陆版本
- `intl`：国际版本
- `end2end`：用于单元测试

## 测试

### 运行单元测试

```bash
./gradlew clean :httpdns-sdk:testEnd2endForTestUnitTest
```

### Demo 应用

SDK 提供了两个 Demo：

#### 1. app module（旧版 Demo）

在 `MyApp.java` 中配置测试账号：

```java
private HttpDnsHolder holderA = new HttpDnsHolder("请替换为测试用A实例的accountId", "请替换为测试用A实例的secret");
private HttpDnsHolder holderB = new HttpDnsHolder("请替换为测试用B实例的accountId", null);
```

> 两个实例用于测试实例间互不影响，体验时只需配置一个

#### 2. demo module（推荐）

使用 Kotlin + MVVM 开发，功能更丰富。在 `demo/build.gradle` 中配置测试账号：

```groovy
buildConfigField "String", "ACCOUNT_ID", "\"请替换为测试用实例的accountId\""
buildConfigField "String", "SECRET_KEY", "\"请替换为测试用实例的secret\""
buildConfigField "String", "AES_SECRET_KEY", "\"请替换为测试用实例的aes\""
```

## 依赖与要求

- Android API 19+（Android 4.4+）
- 需要权限：`INTERNET`、`ACCESS_NETWORK_STATE`

## 安全说明

- 切勿提交真实的 AccountID/SecretKey，请通过本地安全配置或 CI 注入
- 若担心设备时间偏差影响鉴权，可使用 `setAuthCurrentTime()` 校正时间

## 文档

官方文档：[Android SDK手册](https://help.aliyun.com/document_detail/435250.html)

## 感谢

本项目中 Inet64Util 工具类由 [Shinelw](https://github.com/Shinelw) 贡献支持，感谢。
