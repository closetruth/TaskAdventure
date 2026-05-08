
# 🎮 TaskAdventure - 游戏化任务管理系统

一个用于提升执行力的个人工具，通过"任务 + 随机奖励 + 宠物养成 + 自走棋"机制，让枯燥任务变得像游戏一样有反馈。

---

## ✨ Features（功能）

### 📋 核心任务系统

**基础功能：**
- ✅ 创建、开始、暂停、继续、完成任务
- ✅ 实时显示任务运行时间
- ✅ 记录任务的创建时间、完成时间和总耗时
- ✅ 多任务管理，支持任务列表和已完成任务查看

**奖励机制：**
- 🎲 **间歇掉落**：运行中的任务每 **10分钟** 进行一次掉落判定
  - 42% 概率无奖励
  - 58% 概率获得 5-50 金币
  - 10% 概率额外获得 1 钻石
- 💰 **待领取系统**：掉落的奖励累积在任务中，需手动点击"领取奖励"才能加入钱包
- ⏱️ **进度条**：每 10 分钟循环一次，直观显示下一个奖励周期
- 🏆 **完成奖励**：完成任务时获得基于专注时长的大额奖励
  - 基础奖励：每分钟 2 金币 + 每 10 分钟 1 钻石
  - ±20% 随机浮动
  - 20% 概率额外获得 1 钻石

**计划时间系统：**
- ⏰ **设置计划时间**：可以为任务设置目标专注时长（最少 5 分钟）
- 🔔 **自动暂停**：到达计划时间后任务自动暂停
- ⏳ **倒计时显示**：实时显示剩余时间
- 🎯 **强制完成**：必须达到计划时间后才能完成任务
- 💎 **奖励加成**：完成计划时间获得 **50% 额外奖励**！

**任务管理：**
- 🗑️ **删除任务**：未完成的任务可以删除（运行中需先暂停）
- 📊 **任务统计**：查看已完成任务的掉落奖励和时间记录

### 🐾 宠物养成系统

**领养与培养：**
- 🏪 **宠物商店**：使用金币领养 6 种不同类型的宠物
  - 🐱 猫 (50金)、🐶 狗 (80金)、🐰 兔 (60金)
  - 🐉 龙 (200金)、🦊 狐 (100金)、🐼 熊猫 (150金)
- 📈 **等级系统**：宠物通过获得经验升级，升级后恢复满体力并增加快乐值
- 💫 **状态追踪**：实时显示宠物的四项属性

**互动玩法：**
- 🍖 **喂食** (-5金)：增加饱食度 +30、体力 +10、快乐 +5
- 🎾 **玩耍**：大幅增加快乐 +15，获得经验 +10（需要体力≥20、饱食≥10）
- 💪 **训练**：获得大量经验 +25，但降低快乐 -5（需要体力≥30、饱食≥20）
- 😴 **休息**：快速恢复体力 +50，饱食 -10
- 🗑️ **送走**：可以删除不想要的宠物

**自动变化：**
- ⏰ **时间流逝**：每小时饱食 -5、快乐 -3、体力 +2
- 📉 **状态衰减**：长时间不照顾宠物会变饿、变累或不开心
- 🎊 **升级庆祝**：升级时快乐 +20、体力回满

### ♟️ 迷你自走棋

**游戏机制：**
- 🛒 **商店系统**：每回合可购买棋子，刷新商店消耗 3 金币
- ⚔️ **战斗系统**：与敌人进行自动战斗，比较战力决定胜负
- 🔄 **合成升星**：备战席两枚同名同星级棋子可合成升星（最高★★★）
- 🎯 **羁绊加成**：棋盘上同羁绊≥2 枚有战力加成
- ❤️ **生命系统**：连败会受伤，生命归零游戏结束
- 💰 **经济系统**：连胜获得额外金币

**特殊功能：**
- 🔁 **重开新局**：随时可以重新开始
- 💎 **复活**：游戏结束后花费 5 钻石复活继续游戏
- 📊 **战力预览**：显示下一波敌人的预计战力

### 📅 赛季系统

- 📆 **每周结算**：记录每周的任务完成情况
- 🏆 **排位奖励**：根据本周表现获得赛季奖励
- 📈 **数据统计**：查看历史周次的成绩

---

## 🧠 Motivation（为什么做）

传统 ToDo 工具的问题：

* ❌ 没有即时反馈
* ❌ 缺乏奖励机制
* ❌ 很容易放弃
* ❌ 枯燥乏味，难以坚持

本项目尝试用游戏化机制解决：

> “如何让自己更愿意开始任务，并坚持完成？”

**解决方案：**
1. **间歇性奖励**：模仿游戏中的开箱机制，让每次专注都有惊喜
2. **多重玩法**：任务 + 宠物 + 自走棋，形成正向循环
3. **目标导向**：计划时间系统帮助设定和达成目标
4. **视觉反馈**：进度条、倒计时、属性条等直观展示

---

## 🕹️ Core Mechanics（核心机制）

### 1. 奖励获取

**间歇掉落（每10分钟）：**
```
42% 概率 → 无奖励
58% 概率 → 5-50 金币（均匀分布）
10% 概率 → 额外 +1 钻石
```

**完成奖励（基于时长）：**
```
基础金币 = 运行分钟数 × 2
基础钻石 = 运行分钟数 / 10

如果达到计划时间：
    最终奖励 = 基础奖励 × 1.5 （+50%加成）

随机浮动：±20%
额外钻石：20% 概率 +1
```

### 2. 宠物成长

**经验获取：**
- 玩耍：+10 经验
- 训练：+25 经验

**升级需求：**
```
升级到 Lv.N 需要：N × 100 经验
例如：Lv.1→Lv.2 需要 100 经验
      Lv.2→Lv.3 需要 200 经验
```

**状态变化（每小时）：**
- 饱食度 -5
- 快乐值 -3
- 体力 +2（自然恢复）

### 3. 自走棋战斗

**战力计算：**
```
总战力 = Σ(棋子基础战力 × 星级倍率 × 羁绊加成)
```

**胜负判定：**
- 玩家战力 > 敌人力 → 胜利，获得金币
- 玩家战力 < 敌人力 → 失败，扣除生命

**连胜/连败奖励：**
- 连胜：额外金币奖励
- 连败：受伤增加

---

## 🏗️ Tech Stack（技术栈）

**后端：**
- Java 17+
- Spring Boot 3.x
- Spring Data JPA / Hibernate
- H2 Database (文件数据库)
- Maven

**前端：**
- Vue 3 (Composition API)
- Vite
- 原生 CSS

**开发工具：**
- IntelliJ IDEA / VS Code
- Git

---

## 📦 Project Structure（项目结构）

```
TaskAdventure/
├── backend/                    # Spring Boot 后端
│   ├── src/main/java/com/closetruth/
│   │   ├── task/              # 任务系统
│   │   │   ├── TaskEntity.java
│   │   │   ├── TaskService.java
│   │   │   ├── TaskController.java
│   │   │   └── ...
│   │   ├── pet/               # 宠物系统
│   │   │   ├── PetEntity.java
│   │   │   ├── PetService.java
│   │   │   ├── PetController.java
│   │   │   └── ...
│   │   ├── autochess/         # 自走棋系统
│   │   │   ├── AutochessGameService.java
│   │   │   ├── AutochessController.java
│   │   │   └── ...
│   │   ├── season/            # 赛季系统
│   │   │   ├── SeasonService.java
│   │   │   ├── SeasonController.java
│   │   │   └── ...
│   │   └── BackendApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── frontend/                   # Vue 3 前端
│   ├── src/
│   │   ├── App.vue            # 主应用组件
│   │   ├── components/
│   │   │   └── TaskTicker.vue
│   │   └── assets/
│   ├── index.html
│   ├── package.json
│   └── vite.config.js
│
├── data/                       # H2 数据库文件
│   └── gamified.mv.db
│
└── docker-compose.yml
```


---

## 🚀 Getting Started（运行）

### 前置要求

- JDK 17+
- Node.js 18+
- Maven 3.6+

### 后端启动

```bash
cd backend

# 安装依赖
mvn clean install

# 运行应用
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080` 启动

### 前端启动

```bash
cd frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev
```

前端应用将在 `http://localhost:5173` 启动

### 数据库

项目使用 H2 文件数据库，数据存储在 `data/gamified.mv.db`
首次运行时会自动创建表结构。

**重置数据库：**
```bash
# 删除数据库文件
rm data/gamified.mv.db
rm data/gamified.lock.db

# 重启后端，Hibernate 会重新创建表
```

### Docker (可选)

```bash
docker-compose up -d
```

---

## 🔑 Core Logic（核心逻辑示例）

### 任务奖励计算

```java
// 完成奖励计算
long elapsedMinutes = task.getCurrentElapsedSeconds() / 60;
Integer plannedMinutes = task.getPlannedMinutes();

int baseGold = (int) (elapsedMinutes * 2);        // 每分钟 2 金币
int baseDiamonds = (int) (elapsedMinutes / 10);   // 每 10 分钟 1 钻石

// 如果达到计划时间，获得 50% 加成
if (plannedMinutes != null && elapsedMinutes >= plannedMinutes) {
    baseGold = (int) (baseGold * 1.5);
    baseDiamonds = (int) (baseDiamonds * 1.5);
}

// ±20% 随机浮动
int variance = (int) (baseGold * 0.2);
int finalGold = Math.max(10, baseGold + random(-variance, variance));
```

### 宠物升级系统

```java
public void checkLevelUp() {
    int expNeeded = this.level * 100;  // Lv.N 需要 N*100 经验
    if (this.experience >= expNeeded) {
        this.level++;
        this.experience -= expNeeded;
        this.happiness = Math.min(100, this.happiness + 20);  // 升级快乐+20
        this.energy = 100;  // 体力回满
    }
}
```

### 自动暂停机制

```java
public boolean shouldAutoPause() {
    if (plannedMinutes == null || plannedMinutes <= 0) {
        return false;
    }
    long currentElapsed = getCurrentElapsedSeconds();
    long plannedSeconds = plannedMinutes * 60L;
    return currentElapsed >= plannedSeconds;  // 到达计划时间
}
```


---

## 📊 Future Plans（后续计划）

**短期：**
- [ ] 宠物进化系统（达到一定等级后可进化）
- [ ] 更多自走棋棋子和羁绊
- [ ] 成就系统
- [ ] 数据统计面板（每日/每周专注时间图表）

**中期：**
- [ ] 宠物对战 PVP
- [ ] 任务分类和标签系统
- [ ] 番茄钟模式
- [ ] 自定义奖励规则

**长期：**
- [ ] 移动端 App
- [ ] 多人协作任务
- [ ] 公会/社区功能
- [ ] 更多小游戏集成

---

## ⚠️ Notes（注意）

* 🎯 本项目仅用于个人自控与效率提升
* 💰 不涉及真实金钱交易，所有金币和钻石均为虚拟道具
* 🎮 奖励机制已做简化，避免成瘾设计
* 📱 目前仅支持 Web 端访问
* 🔒 数据存储在本地，建议定期备份 `data/` 目录
* ⚖️ 请合理使用，保持工作与休息的平衡

---

## 📜 License

MIT

---

## 💡 Inspiration（灵感）

**游戏机制：**
* 🎲 游戏中的“开宝箱机制” - 间歇性奖励带来的惊喜感
* 📱 抖音的滑动反馈 - 即时满足的设计哲学
* ⚔️ 皇室战争的卡牌收集 - 成长与收集的快感
* ⛏️ 泰拉瑞亚的探索奖励 - 未知带来的动力

**心理学原理：**
* 🧠 行为心理学中的“间歇性奖励” - 最强的强化方式
* 🎯 目标设定理论 - SMART 原则的应用
* 🔄 习惯养成回路 - 提示→行动→奖励
* 💪 自我决定理论 - 自主性、胜任感、归属感

**效率方法：**
* 🍅 番茄工作法 - 专注与休息的节奏
* ✅ GTD (Getting Things Done) - 任务管理方法论
* 📊 量化自我 - 用数据驱动改进
