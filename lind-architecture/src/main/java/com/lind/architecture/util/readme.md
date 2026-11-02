# 知识点 (1)：Fluent / Functional Builder

- `ObjectBuilder<T>`：统一 `build()`
- `ApiTypeHelper.resolve(fn, builder)`：把 `Function<Builder, ObjectBuilder<T>>` 收成不可变对象

对照 ES：`c -> c.query(q -> q.term(...))`
