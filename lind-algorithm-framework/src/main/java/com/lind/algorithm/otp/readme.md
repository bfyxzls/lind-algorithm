# otp 包（com.lind.algorithm.otp）

一次性密码：`HOTP`（RFC 4226）与 `TOTP`（RFC 6238），纯 JDK（`javax.crypto`）。

## 类一览

| 类 | 说明 |
|---|---|
| `Hotp` | 基于计数器的 OTP；验证支持滑动窗口 |
| `Totp` | 基于时间步的 OTP；验证为滑动时间窗口 |
| `HmacAlgorithm` | `SHA1` / `SHA256` / `SHA512` |
| `Base32` | 密钥 Base32 编解码（Authenticator 常见格式） |

## TOTP 滑动窗口是什么

时间被切成固定步长（默认 30 秒）的时间步 `T = floor(epochMillis / period)`。  
生成码时用当前 `T`；**验证**时在 `[T - window, T + window]` 内逐个时间步比对。

```text
window = 1 时（默认）：

  T-1          T           T+1
  |------------|------------|
     上一窗口码仍有效 ← 当前 → 下一窗口码也接受
```

因此：用户拿到的码在「指定滑动窗口」覆盖的时间步内都算有效（时钟略偏、跨步边界仍可用）。

| 参数 | 默认 | 说明 |
|---|---|---|
| `digits` | 6 | 验证码位数，可设 6–8 |
| `periodSeconds` | 30 | 时间步长（秒） |
| `window` | 1 | 滑动窗口半径（步数）；`0` 表示仅当前步 |
| `algorithm` | SHA1 | HMAC 算法 |

## 示例

```java
byte[] secret = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);

// 默认 6 位、30s、窗口 ±1
Totp totp = new Totp(secret);
String code = totp.now();
boolean ok = totp.verify(code);

// 自定义：8 位、60 秒步长、窗口 ±2
Totp custom = new Totp(secret, 8, 60, HmacAlgorithm.SHA256, 2);

// Base32 密钥（Google Authenticator）
Totp ga = Totp.fromBase32Secret("JBSWY3DPEHPK3PXP");
String uri = ga.otpAuthUri("MyApp", "user@example.com", "JBSWY3DPEHPK3PXP");
```

## HOTP

```java
Hotp hotp = new Hotp(secret, 6, HmacAlgorithm.SHA1, 1); // 窗口 ±1 计数器
String code = hotp.generate(counter);
boolean ok = hotp.matches(code, counter);
```

## 注意

- 密钥请用安全随机数生成并妥善保管；勿把明文 secret 提交到仓库。
- 生产环境建议对成功验证的时间步做「防重放」（同一码不可重复使用）。
- 与限流里的滑动窗口不同：这里窗口单位是 **时间步/计数器步**，不是请求时间戳队列。
