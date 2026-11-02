# 知识点 (3)：可替换 JsonMapper

- `JsonMapper`：协议层抽象
- `JacksonJsonMapper`：默认
- `SimpleJsonMapper`：手写极简实现，证明换库只改装配

`write(Object, Writer)` 为何收 `Writer` 而不是只返回 `String`？见 [`../io/readme.md`](../io/readme.md)。
