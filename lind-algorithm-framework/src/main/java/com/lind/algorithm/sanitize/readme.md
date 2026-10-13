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

默认去掉（大小写不敏感）：`mozilla`、`applewebkit`、`webkit`、`chrome`、`chromium`、`safari`、`firefox`、`gecko`、`khtml`、`version`、`mobile`、`crios`、`edg`、`edge`、`edga`、`edgios`、`opr`、`opera`、`electron`、`msie`、`trident`、`dalvik`，以及纯版本号片段；保留如 `MyApp/1.0`、`BizChannel/xxx` 等业务标识。
