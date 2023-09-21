# httpdns

请参考官网文档，了解httpdns功能。[https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX](https://help.aliyun.com/product/30100.html?spm=a2c4g.750001.list.154.2d0d7b13T0aYuX)

## 注意

productFlavors中normal是中国大陆版本，intl是国际版本，end2end用于单元测试

### 配置初始IP
请在gradle.properties中配置你所使用账号的初始服务IP，否则无法请求
```gradle
# 大陆默认配置
# 默认 region
REGION=""
# 默认 初始IP（v4）InputYourInitServerIp 替换为启动IP, 有几个就放几个
INIT_SERVERS={"InputYourInitServerIp", "InputYourInitServerIp", "InputYourInitServerIp"}
# 默认 初始IP（v6）InputYourInitServerIp 替换为启动IP, 有几个就放几个
IPV6_INIT_SERVERS={"InputYourInitServerIp"}
# 国际版默认配置
# 默认 region
INTL_REGION="sg"
# 默认 初始IP（v4）InputYourInitServerIp 替换为启动IP, 有几个就放几个
INTL_INIT_SERVERS={"InputYourInitServerIp", "InputYourInitServerIp", "InputYourInitServerIp"}
# 默认 初始IP（v6）InputYourInitServerIp 替换为启动IP, 有几个就放几个
INTL_IPV6_INIT_SERVERS={"InputYourInitServerIp"}
# 缺省 调度IP v4，在初始IP不可用时，启用。InputYourDefaultUpdateServerIp 替换为兜底调度IP, 有几个就放几个
INTL_UPDATE_SERVERS={"InputYourDefaultUpdateServerIp"}
# 缺省 调度IP v6，在初始IP不可用时，启用。InputYourDefaultUpdateServerIp 替换为兜底调度IP, 有几个就放几个
INTL_IPV6_UPDATE_SERVERS={"InputYourDefaultUpdateServerIp"}
```

### 配置Demo测试账号

SDK提供了两个Demo，分别是app module和demo module。

app module是旧版demo，测试时需要在 MyApp.java 中需要配置测试账号

```java
    private HttpDnsHolder holderA = new HttpDnsHolder("请替换为测试用A实例的accountId", "请替换为测试用A实例的secret");
    private HttpDnsHolder holderB = new HttpDnsHolder("请替换为测试用B实例的accountId", null);
```
> 这里两个实例是为了测试实例之间互不影响，体验只用配置一个

demo module中是全新module，使用Kotlin + MVVM开发，功能更丰富，建议测试时使用新demo进行测试，测试时需要在demo/build.gradle中配置测试账号

```groovy
buildConfigField "String", "ACCOUNT_ID", "\"请替换为测试用B实例的accountId\""
buildConfigField "String", "SECRET_KEY", "\"请替换为测试用A实例的secret\""
```

### 运行测试case

```
// unit test
./gradlew clean :httpdns-sdk:testEnd2endForTestUnitTest
```

## 感谢
本项目中Inet64Util工具类 由[Shinelw](https://github.com/Shinelw)贡献支持，感谢。
