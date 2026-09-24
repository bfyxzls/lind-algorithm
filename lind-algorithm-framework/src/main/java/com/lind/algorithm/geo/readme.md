# geo 包（com.lind.algorithm.geo）

| 类 | 说明 |
|---|---|
| `Geohash` | 经纬度 ↔ Geohash 编码、邻格 |
| `LatLon` | 纬度/经度 record |

```java
String hash = Geohash.encode(39.9042, 116.4074, 6); // 北京
LatLon point = Geohash.decode(hash);
List<String> near = Geohash.neighbors(hash);
```

适合 LBS 粗筛：先按 Geohash 前缀或邻格收窄候选，再算精确距离。
