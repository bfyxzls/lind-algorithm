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

## 服务间调用（A → B）能用吗？

**可以，且通常比「每次请求带明文共享密钥 / 静态 API Key」更安全**，但不是服务间鉴权的终极方案。

| 对比 | 静态共享密钥上送 | TOTP 上送 |
|---|---|---|
| 链路上传什么 | 长期有效的密钥本身 | 仅短时 OTP（默认数十秒级） |
| 泄露后果 | 被截获即可无限调用，直到轮换密钥 | 截获的码很快过期；窗口内需再加防重放 |
| 密钥是否出域 | 每次请求都出域 | 密钥只留在 A/B 本地 |
| 前提 | 无 | A/B 时钟大致同步（NTP） |
| 仍不足 | — | 密钥泄露后攻击者仍能算码；6 位需配限流；未绑定具体请求内容 |

**结论**：相对「Header 里塞同一个 secret」，TOTP 更好（密钥不出网、码有时效）。  
若要求更高，优先考虑：**请求签名（HMAC 含 method/path/body/timestamp）** 或 **mTLS**。TOTP 适合作为轻量升级，或叠加在已有网关鉴权上。

推荐服务间用法：

- 位数用 **8**，算法用 **SHA256**
- `window=1` 容忍少量时钟偏差
- 服务端对成功时间步做 **防重放**（见 `TotpServiceAuthenticator`）
- 对失败校验做 **限流**，防 8 位码暴力猜

```java
// 服务 A
Totp client = new Totp(sharedSecret, 8, 30, HmacAlgorithm.SHA256, 1);
headers.put("X-Client-Id", "service-a");
headers.put("X-Totp", client.now());

// 服务 B
TotpServiceAuthenticator auth = new TotpServiceAuthenticator(sharedSecret, 8, 30, HmacAlgorithm.SHA256, 1);
auth.authenticate("service-a", headers.get("X-Totp"));
```
