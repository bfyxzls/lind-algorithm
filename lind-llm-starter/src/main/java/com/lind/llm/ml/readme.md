# 机器学习（`com.lind.llm.ml`）学习笔记

本包用 **纯 Java** 实现一组最小但完整的 ML 积木，帮助理解训练、损失、梯度与超参——这些同样是大模型微调 / 推理调参的基础。

## 1. 机器学习在做什么？

给定数据 $D=\{(x_i, y_i)\}$，学习一个函数 $f_\theta(x)$，使预测 $\hat{y}=f_\theta(x)$ 接近真实 $y$。

- $\theta$：**参数**（权重、偏置）——靠训练「学」出来  
- 学习率、epoch、正则强度等：**超参数**——通常人工 / 调度器设定，可在训练中动态调整  

| 类型 | 标签 $y$ | 本包示例 |
|---|---|---|
| 回归 | 连续值 | `LinearRegression` |
| 分类 | 类别 | `LogisticRegression`（二分类） |
| 聚类 | 无标签 | `KMeans` |

## 2. 训练闭环（强烈建议按此顺序阅读代码）

```
数据 → 特征缩放 → 划分 train/test
              ↓
        初始化模型参数 θ
              ↓
   ┌── epoch 循环 ──────────────────┐
   │  前向：ŷ = fθ(x)                │
   │  损失：L(ŷ, y)                  │
   │  反向：∇θ L                     │
   │  更新：θ ← θ − η · ∇θ L         │
   │  （可选）动态改 η / 早停         │
   └─────────────────────────────────┘
              ↓
           在 test 上评估
```

对应代码：

| 步骤 | 类 |
|---|---|
| 样本 | `data.LabeledPoint` |
| 缩放 | `data.StandardScaler` |
| 划分 | `data.Datasets` |
| 损失 / 激活 | `math.Losses` / `math.Activations` |
| 模型 | `model.*` 实现 `Trainable` |
| 训练循环 + 超参 | `train.Trainer` / `train.HyperParameters` |
| 指标 | `metrics.*` |

## 3. 核心公式（对照实现）

### 3.1 线性回归

$$
\hat{y} = w^\top x + b,\quad
L = \frac{1}{2n}\sum_i (\hat{y}_i - y_i)^2
$$

梯度（对单个样本简化）：$\partial L/\partial w = (\hat{y}-y)\,x$，$\partial L/\partial b = \hat{y}-y$。

### 3.2 逻辑回归（二分类）

$$
p = \sigma(w^\top x + b),\quad
\sigma(z)=\frac{1}{1+e^{-z}},\quad
L = -[y\log p + (1-y)\log(1-p)]
$$

预测类别：$p \ge 0.5 \Rightarrow 1$，否则 $0$。

### 3.3 K-Means

反复：样本归到最近中心 → 中心改为簇内均值，直到收敛或达最大迭代。

## 4. 超参动态调整（为 LLM / 在线服务打样）

`HyperParameters` 是可变对象：`Trainer` 每个 epoch 开始会读取当前 `learningRate()`。你可以：

- 训练中途 `hp.learningRate(hp.learningRate() * 0.5)`（学习率衰减）  
- 设置 `earlyStoppingPatience`，验证损失不再下降则停  

这对应业务里常见的：**不重启进程就改温度 / top-p / 学习率调度**——先在小模型上建立心智模型。

## 5. 实践建议

1. 先跑通 `LinearRegressionTest`，看 loss 是否单调下降  
2. 关掉 `StandardScaler` 对比收敛难度（体会特征尺度的重要性）  
3. 把学习率改成 `10` 或 `1e-6`，观察发散或几乎不动  
4. 再读 `LogisticRegression` / `KMeans`，对照上面公式  

## 6. 与大模型的映射（预习）

| 本包 | LLM 世界 |
|---|---|
| `w, b` | Transformer 亿级权重、LoRA 低秩矩阵 |
| `learningRate` | AdamW lr、warmup / cosine schedule |
| `epochs` | 预训练 step、SFT epoch |
| `predict(x)` | `generate(prompt)` / chat completion |
| `earlyStopping` | 验证 perplexity 早停、RLHF 奖励平台期 |

下一步（模块路线图 P1）：在 `com.lind.llm.llm` 中抽象「推理客户端」，把「调超参」从训练集延伸到在线请求。
