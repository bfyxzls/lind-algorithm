# sanitize 包（com.lind.algorithm.sanitize）

文本 / 请求数据清洗：抽象基类 + 多种实现，可单独使用或串成管道。

| 类 | 说明 |
|---|---|
| `TextSanitizer` | 抽象基类：`sanitize` / `type` |
| `HtmlTagSanitizer` | 去 HTML / script / style，可选实体解码 |
| `PhoneNumberSanitizer` | 去或掩码手机号、固话 |
| `EmailSanitizer` | 去或掩码邮箱 |
| `UserAgentSanitizer` | 去掉浏览器通用 UA，保留业务 UA |
| `WhitespaceSanitizer` | 空白规范化 |
| `InvisibleCharSanitizer` | BOM / 零宽 / 控制字符 |
| `IdCardSanitizer` | 身份证掩码或删除 |
| `BankCardSanitizer` | 银行卡掩码或删除 |
| `UrlSanitizer` | 去掉 http(s) 链接 |
| `CompositeTextSanitizer` | 按序组合多种清洗 |
| `TextSanitizers` | 工厂与常用管道 |

```java
// 单种清洗
String plain = TextSanitizers.html().sanitize("<b>hi</b>");
String ua = TextSanitizers.userAgent().sanitize(requestUserAgent);

// 自定义替换
new PhoneNumberSanitizer("****").sanitize("手机13800138000");

// UGC 入库管道
String safe = TextSanitizers.ugcPipeline().sanitize(userContent);

// 日志脱敏管道
String masked = TextSanitizers.privacyPipeline().sanitize(logLine);

// 自定义组合
TextSanitizer pipeline = new CompositeTextSanitizer(
    new HtmlTagSanitizer(),
    new PhoneNumberSanitizer(),
    new EmailSanitizer(),
    new WhitespaceSanitizer());
```

## User-Agent 清洗规则

识别 `Name/version` 与无斜杠的裸名，过滤浏览器 / 系统噪声后保留业务名：

```text
... Qoder/0.4.4 Chrome/... Electron/... Safari/...  →  Qoder
... Qoder Chrome/... Electron/... Safari/...       →  Qoder   （无 /version 同样提取）
```

噪声包括浏览器引擎（`mozilla`、`chrome`、`electron`、`safari`…）以及系统片段（`windows`、`nt`、`win64`、`x64`、`like`、`linux`、`android`…）。纯浏览器 UA 得到空串；`new UserAgentSanitizer(true)` 对带版本的产品保留 `Qoder/0.4.4`。
