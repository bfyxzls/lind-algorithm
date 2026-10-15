# lind-llm-starter

面向 **AI 大模型 / 机器学习** 的学习与研究模块：用纯 JDK 演示经典 ML 概念、训练循环与超参动态调整；后续再扩展 LLM 推理、微调与参数服务化。

> 当前阶段重点：`com.lind.llm.ml`（机器学习基础）。LLM 相关包将按路线图逐步加入。

## 为什么先做 ML？

大模型（LLM）的训练 / 微调 / 推理，底层仍建立在机器学习共同语言之上：

| 概念 | 在经典 ML 中 | 在 LLM 中 |
|---|---|---|
| 损失函数 | MSE / 交叉熵 | 下一词预测交叉熵、RLHF 奖励等 |
| 优化器 | SGD / 动量 | AdamW、学习率调度 |
| 超参 | 学习率、epoch、batch | 温度、top-p、LoRA rank、学习率 |
| 过拟合 | 正则、早停 | dropout、数据配比、早停 |
| 评估 | Accuracy / F1 / MSE | perplexity、人工评测、基准集 |

先把这些「积木」在小规模数据上跑通，再学 Transformer / 微调会轻松很多。

## 包结构（规划）

```
com.lind.llm
├── ml/                 # ✅ 已落地：经典机器学习（纯 JDK）
│   ├── math/           # 向量、激活、损失
│   ├── data/           # 样本、标准化、划分
│   ├── model/          # 线性回归、逻辑回归、K-Means
│   ├── train/          # 训练循环、超参、早停
│   └── metrics/        # 回归 / 分类指标
├── llm/                # 🔜 大模型：Tokenizer、Prompt、推理客户端抽象
├── finetune/           # 🔜 微调 / LoRA / 参数高效适配概念与实验脚手架
└── serve/              # 🔜 模型参数动态下发、A/B、热更新（与业务结合）
```

## 快速开始

```bash
# 编译 + 运行 ml 单测
mvn -pl lind-llm-starter -am test

# 仅本模块
mvn -pl lind-llm-starter test
```

依赖：

```xml
<dependency>
    <groupId>com.lind</groupId>
    <artifactId>lind-llm-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## `ml` 包能学到什么

详见 [`ml/readme.md`](src/main/java/com/lind/llm/ml/readme.md)。摘要：

1. **监督 / 无监督**：线性回归、逻辑回归、K-Means  
2. **训练要素**：特征缩放、train/test 划分、损失、梯度下降  
3. **超参动态调整**：训练中修改学习率 / 提前停止（为后续「模型参数热更新」打样）  
4. **评估**：MSE、R²、Accuracy、Precision / Recall / F1  

## 最小示例

```java
import com.lind.llm.ml.data.LabeledPoint;
import com.lind.llm.ml.data.StandardScaler;
import com.lind.llm.ml.model.LinearRegression;
import com.lind.llm.ml.train.HyperParameters;
import com.lind.llm.ml.train.Trainer;

List<LabeledPoint> data = List.of(
    new LabeledPoint(new double[]{1}, 2.1),
    new LabeledPoint(new double[]{2}, 4.0),
    new LabeledPoint(new double[]{3}, 6.2)
);

StandardScaler scaler = StandardScaler.fit(data);
List<LabeledPoint> scaled = scaler.transform(data);

HyperParameters hp = HyperParameters.defaults().learningRate(0.1).epochs(500);
LinearRegression model = new LinearRegression(1);
Trainer.fit(model, scaled, hp);

double yHat = model.predict(scaler.transformFeatures(new double[]{4}));
```

## 路线图

| 阶段 | 内容 |
|---|---|
| ✅ P0 | `ml`：回归 / 分类 / 聚类 + 训练与指标 + 文档 |
| P1 | `llm`：HTTP 推理客户端抽象（OpenAI 兼容）、Prompt 模板、流式输出钩子 |
| P2 | `finetune`：参数高效微调概念代码（LoRA 矩阵形状示意）、数据集管线 |
| P3 | `serve`：运行时超参 / 权重版本热切换、与 Spring Boot 自动配置打通 |

## 设计原则

- **可运行、可单测**：每个核心算法有对应测试，不堆「只读不跑」的伪代码  
- **先概念后框架**：不绑定 PyTorch / TensorFlow；需要时再引入 DJL 等  
- **为 LLM 留接口**：`Trainable`、`HyperParameters`、`Metrics` 等抽象后续可复用  
