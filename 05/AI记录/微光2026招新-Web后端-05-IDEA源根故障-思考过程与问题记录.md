# 微光2026招新-Web后端-05 · 附：IDEA 源根故障 · 思考过程与问题记录

> 这一份和前面三份不一样：它记的**不是作业题目，而是一次工程配置事故**。
>
> 起因是 02 和 05 都有一个 `Student` 类，为了拆模块动了 `.idea/` 下的配置；改完之后 IDEA 的**源根全部失效**——右键不能新建 Java 类、也标记不了源目录。整个排查跨了两个会话、六七个来回。
>
> 我把过程完整记下来，因为「IDE 看起来坏了」这一类问题最耗时间：症状指向工具，而工具恰恰是你最没把握的那一环。
>
> 环境：IntelliJ IDEA 2026.2.2 / JDK 27 / Windows 11

---

## 一、起因：两个 `Student`，一个 `duplicate class`

`02/src/Student.java` 和 `05/src/Student.java` 都是**默认包下的 `public class Student`**。当时它们在同一个模块里，各自是一个源根，编译产物都往 `out/production/2/` 里放——于是撞车。

**报错原文（复现输出）**

```
05\src\Student.java:1: 错误: 类重复: Student
public class Student {
       ^
1 个错误
```

（Windows 控制台的原始输出是 GBK 花屏 `05\src\Student.java:1: ����: ���ظ�: Student`。）

对照组：**单独编译其中一个完全通过**——只有两个同名类被放进同一个编译单元集合里才会撞。

**我当时的原话**：

> 这个重复类的问题有没有办法让两个类屏蔽，因为改名字不是很方便

给的三个方案：

| 方案 | 做法 | 代价 |
| --- | --- | --- |
| A | 把 `02/src` 标成 Excluded | 02 的代码就不再参与编译了，等于半放弃 |
| B | **每个 `0N` 拆成一个独立模块** | 配置改动大，但最干净 |
| C | 加 `package` | 要改所有文件的包名和目录结构 |

我选了 **B**。

**事后看，方案本身没错**——它确实是"让同名的两个类各自独立"的正解。**错的是我操作它的方式**（见下）。

---

## 二、时间线

| 时间 | 发生了什么 |
| --- | --- |
| 17:03 | 在 IDEA **开着**的时候，手写 `.idea/02~05.iml`（内容是 `file://$PROJECT_DIR$/0N`），改写 `modules.xml`，删掉旧的 `2.iml` |
| 17:04:01 | IDEA 热重载，日志里第一轮 `Watch roots should be absolute`（12 条） |
| — | **我重启了 IDEA** |
| 17:12:55 | 日志：`Workspace model loaded from cache.` |
| 17:12:56 | 日志：**第二轮** `Watch roots should be absolute`（同样 12 条） |
| 17:16 | 我问：为什么设置不了源目录了 |
| ~18:50 | 第一次判断错误，被否掉；翻日志找到硬证据 |
| 18:53 | 我点破一个关键约束（见问题 5） |
| 18:54 | 改成绝对路径；IDEA 热重载**毫无反应**，盯 80 秒日志 0 条 |
| 18:58 | `Ctrl + Alt + Y` 按了没用（空按） |
| 19:01 | 改用 `Ctrl+Shift+A` → `Reload All from Disk`，回「src 正常了」 |
| 19:0x | `Ctrl + F9` Build，四个模块各自编译成功 |

**这张表里最重要的一行是 17:04:01 和 17:12:56 那两轮**——下面会讲。

---

## 三、逐个问题记录

### 问题 1：手写的 `.iml` 长这样

```xml
<?xml version="1.0" encoding="UTF-8"?>
<module type="JAVA_MODULE" version="4">
  <component name="NewModuleRootManager" inherit-compiler-output="false">
    <output url="file://$PROJECT_DIR$/out/production/05" />
    <output-test url="file://$PROJECT_DIR$/out/test/05" />
    <exclude-output />
    <content url="file://$PROJECT_DIR$/05">
      <sourceFolder url="file://$PROJECT_DIR$/05/src" isTestSource="false" />
    </content>
    <orderEntry type="inheritedJdk" />
    <orderEntry type="sourceFolder" forTests="false" />
  </component>
</module>
```

用了 `$PROJECT_DIR$` 宏——参照 IDEA 自己生成的 `.iml` 里 `file://$MODULE_DIR$/src` 的写法，我以为宏会被自动展开。

写完只验证了两件事：**XML 格式合法、路径确实存在**。然后让用户「Reload All from Disk，或者重启 IDEA」。

**这里第一个错误：在 IDEA 开着的时候改它的配置文件。**

### 问题 2：重启之后，源根全废了

用户的原话：

> 刚刚是让你改了xml之类的文件，因为02和05都有Student类共用一个class，然后改完了我重启idea，现在有没有问题，而且为什么我现在设置不了源目录了

症状：**右键新建不了 Java 类**，也**标记不了源目录**。

我追问之后，用户又补了一句把我第一次判断直接否掉：

> 不行啊，肯定不是源目录，我都不能新建类文件

### 问题 3：找到硬证据——IDEA 日志里那 12 条

日志的位置：`%LOCALAPPDATA%\JetBrains\IntelliJIdea2026.2\log\idea.log`

**原文（逐字）**

```
2026-10-01 17:04:01,062 [7288713]   WARN - #c.i.o.v.i.l.WatchRootsManager - invalid watch root
java.nio.file.InvalidPathException: Watch roots should be absolute: $PROJECT_DIR$/02
	at com.intellij.openapi.vfs.impl.local.WatchRootsManager.prepareWatchRoot(WatchRootsManager.java:352)
	at com.intellij.openapi.vfs.impl.local.WatchRootsManager.updateWatchRoots(WatchRootsManager.java:303)
	at com.intellij.openapi.vfs.impl.local.WatchRootsManager.replaceWatchedRoots(WatchRootsManager.java:94)
	at com.intellij.openapi.vfs.impl.local.LocalFileSystemImpl.replaceWatchedRoots(LocalFileSystemImpl.java:280)
	at com.intellij.openapi.roots.impl.ProjectRootManagerComponent.postCollect(ProjectRootManagerComponent.kt:261)
```

**同一个错误一共 12 条路径**，也就是 02~05 四个模块的内容根和它们的两个输出目录，全都中招：

```
$PROJECT_DIR$/02      $PROJECT_DIR$/03      $PROJECT_DIR$/04      $PROJECT_DIR$/05
$PROJECT_DIR$/out/production/02 … 03 … 04 … 05
$PROJECT_DIR$/out/test/02       … 03 … 04 … 05
```

**这行日志是整个排查里唯一的硬证据**，它一句话解释了两个症状：

> **那个 `$PROJECT_DIR$` 字符串根本没被替换成 `D:/java/2`，被原封不动当成了一条路径。**

于是：模块在 `modules.xml` 里是登记过的，但**内容根指向一个不存在的路径** → `02/03/04/05` 四个目录**不属于任何模块** → 全都不是源根 → 右键建不了类、标记不了源目录。

看起来像 IDE 坏了，其实是模块的内容根没解析出来。

### 问题 4：重启 IDEA 没用——时间线说明了一切

**我当时的印象是"重启解决/加重了问题"**，日志里那两轮记录把这件事说清楚了：

```
17:04:01  第一轮 Watch roots should be absolute     ← 改完配置、热重载时
17:12:55  Workspace model loaded from cache.        ← 重启后的第 994 毫秒
17:12:56  第二轮 Watch roots should be absolute     ← 重启后的第 1761 毫秒
```

**重启前后各报一轮，一条不多一条不少。** 而重启时那句 `Workspace model loaded from cache.` 是关键——IDEA 不是重新读 `.iml`，而是**直接加载它自己的 workspace 模型缓存**，那个坏掉的模型就躺在缓存里。

**所以重启不是解药，它只是把坏模型原样搬了过来。** 这一条值得单独记住：**IDE 的配置问题，重启经常是最没用的操作之一**，因为配置状态本身被缓存了。

### 问题 5：想改文件，却动不了——一个死循环

诊断出来之后，下一步自然是改 `.iml`。但这里撞上两个约束：

**约束一**：IDEA 开着的时候改它的配置文件，它退出时会用内存里的模型**覆盖回去**。

**约束二**（用户点破的，这句我当时真没想到）：

> 但是你是我部署在idea里的，关了我就不能告诉你了

**关掉 IDEA 就等于断线**——这个对话就跑在 IDEA 的插件里。所以"先关 IDEA 再改配置"这条路根本走不通。

那就只剩热改。可改完发现：**盯了 80 秒日志，一条新记录都没有**，`project-model-cache/cache.data` 的时间戳（18:54:44）比改文件的时间还早。

**原因还是那 12 条坏掉的 watch root**：模块内容根全是无效路径 → IDEA **没有任何可监视的根目录** → 磁盘上的改动它一概感知不到 → 不会热重载。

**成了一个死循环**：坏配置导致文件监视瘫痪，文件监视瘫痪又导致改不动坏配置。

**收获**：**一个组件坏掉，会让"修复它"这件事本身失去反馈。** 这种情况下唯一的出路是找到一条**不依赖监视机制**的强制路径（下面那个 Find Action），而不是反复重试那个已经失效的通道。

### 问题 6：`Ctrl + Alt + Y` 按了没反应

我先让用户按 `Ctrl + Alt + Y`（Reload All from Disk）。用户回了「按了」，但日志里依然没有任何重载记录。

**IDEA 2026.2 把这个动作从 "Synchronize" 改名成了 "Reload All from Disk"，快捷键有变动**——那一下是按空了。

改用**必达路径**：

```
Ctrl + Shift + A   →   输入 Reload All from Disk   →   回车
```

`Ctrl + Shift + A` 是 Find Action，**按名字搜索动作**，不依赖快捷键绑定。用户执行后回了一句「src 正常了」。

**收获**：**快捷键是包装层，动作名才是本体。** 当"我明明按了"和"什么都没发生"同时成立时，换 Find Action 直接用名字调，比继续查快捷键快得多。

### 问题 7：根因到底是什么——一次自我纠错

排查中途我对用户下过一个结论：

> `.iml` 只认 `$MODULE_DIR$`，写 `$PROJECT_DIR$` 是不会被展开的。

**这个结论是错的。** 后来去 IDEA 安装目录里翻了 jar：

```
/c/Program Files/JetBrains/IntelliJ IDEA 2026.2.2/lib/jps-model.jar
  └── org/jetbrains/jps/model/serialization/PathMacroUtil.class
```

里面 dump 出来的常量：

```
PROJECT_DIR_MACRO_NAME
PROJECT_DIR
MODULE_DIR_MACRO_NAME
MODULE_DIR
$MODULE_DIR$
$MODULE_WORKING_DIR$
```

**`$PROJECT_DIR$` 是合法的 JPS 宏**，`PathMacroUtil` 里明确定义了它。所以「宏本身不合法」这个解释站不住——宏合法，但它**没被展开**。

**修正后的判断**（注意：这一段是推断，不是硬证据）：

> 更可能的链条是：**在 IDEA 开着的时候改配置文件 → 17:04 的热重载把模块模型写坏了（展开宏那一步没走对）→ 坏模型进了 workspace 缓存 → 17:12 重启又 `Workspace model loaded from cache` 原样加载回来。**

支撑这条推断的是时间线（问题 4）：如果只是"配置里写了个不认识的宏"，重启理应重新解析文件、结果应该一致或变好；但事实是**重启前后一模一样的两轮报错**，说明坏的不是文件解析这一层，而是被缓存下来的模型。

**坦白说，这条链条我没有 100% 证实**——没有去反编译 `ModuleBridgeLoaderService` 看它到底怎么把宏写坏的。但两个事实是确定的：

1. **日志里的报错文本是硬证据**：`$PROJECT_DIR$` 确实没被展开。
2. **重启无效是硬证据**：坏状态被缓存了，重启把它原样搬了回来。

**收获**：排查时**结论要标清楚哪一条是观测、哪一条是解释**。我这次就是把一个"看起来合理的解释"当成了"结论"讲出去，后来还得自己推翻一次。好在那个解释的**修复方案**（改绝对路径）恰好也有效——但"修好了"不等于"当时说对了"。

### 问题 8（附带）：类名 `File` 会遮蔽 `java.io.File`

排查重名类的时候顺手验了一把，同一类问题的另一个版本：

```
=== 尝试 import java.io.File ===
File.java:1: 错误: 已在该编译单元中定义File
import java.io.File;
       ^
1 个错误

=== 用全限定名 java.io.File ===
编译通过
```

类名和 JDK 常用类重名（`File`、`List`、`Date`…）时，**在这个文件里不能用 import，只能写全限定名**。本题的 `File.java` 因为从没 import 过 `java.io.File`，所以一直没爆。

---

## 四、修复

**两步，全程没有关 IDEA。**

**第一步：把 4 个 `.iml` 里的路径全换成绝对路径**，绕开宏：

```xml
<content url="file://D:/java/2/05">
  <sourceFolder url="file://D:/java/2/05/src" isTestSource="false" />
</content>
<output url="file://D:/java/2/out/production/05" />
<output-test url="file://D:/java/2/out/test/05" />
```

**第二步**：`Ctrl + Shift + A` → 输入 `Reload All from Disk` → 回车。

---

## 五、验证

**① 用户侧的直观确认**

> src 正常了

**② 日志侧的硬证据**

`19:00` 之后再没出现过一条 `Watch roots should be absolute`——**此前是稳定复现的 24 条**（12 条路径 × 2 轮）。

**③ 决定性的功能验证：Build Project（`Ctrl + F9`）**

四个模块**各自独立编译成功**，`out/production/02`、`03`、`04`、`05` 四个输出目录被建了出来——**目录出现本身就是"源根认对了"的证明**。重名类的问题也随之消失。

**④ 顺带清理**

删掉了 `out/production/2/`（旧模块的产物）和 `02/src/` 下两个 9 月 20 号 javac 留下的 `.class` 文件——**编译产物混在源码目录里，正是当时被误开、还触发过 IDE 报错的东西**。

---

## 六、概念小结

| 概念 | 一句话记住 |
| --- | --- |
| 模块内容根 | `.iml` 里的 `<content>` 指向哪里，哪里才属于这个模块；指向无效路径 = 目录不属于任何模块 = **不是源根** |
| 症状与病因的距离 | 「右键不能新建 Java 类」看起来是 IDE 坏了，实际是**模块配置没解析出来**。别在症状层反复试 |
| 唯一的硬证据 | `Watch roots should be absolute: $PROJECT_DIR$/0N`——日志里这一行同时解释了"不能建类"和"标记不了源根" |
| 重启不是解药 | `Workspace model loaded from cache.` 说明重启加载的是**缓存的坏模型**；重启前后报错一模一样就是铁证 |
| 改配置文件要挑时机 | IDEA 开着改配置，退出时可能用内存里的模型覆盖回去 |
| 文件监视会跟着瘫 | watch root 全无效 → IDEA 收不到任何磁盘改动 → 改 `.iml` 它毫无反应（**日志 0 条也是证据**） |
| 死循环 | 坏配置 → 监视瘫痪 → 改不动配置。破局靠一条不依赖监视的通道 |
| 快捷键 vs 动作名 | `Ctrl+Alt+Y` 在 2026.2 已变；`Ctrl+Shift+A`（Find Action）按名字调，永远可达 |
| 观测 vs 解释 | 报错原文和"重启无效"是观测；"热重载写坏了模型"是解释。**讲出去时要分清** |
| 修好了 ≠ 说对了 | 改绝对路径有效，是因为它绕开了问题；不代表当初那个"宏不合法"的解释是对的 |
| 编译产物别混进源码目录 | `src/` 下的 `.class` 会被 IDE 误当成源文件打开，也会干扰排查 |
