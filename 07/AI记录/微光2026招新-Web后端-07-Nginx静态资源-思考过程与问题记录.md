# 微光2026招新-Web后端-07 · Nginx 静态资源（Part3-Nginx） · 思考过程与问题记录

> 这一份记的是 **Part3-Nginx** 这一部分：从读懂「为什么需要 Nginx」那一整段材料，到最后用 `location / { root ...; index ...; }` 把自建的菠萝页服务出来——想过什么、错在哪、最后怎么改对的。
>
> Part1-MySQL 的书籍库设计、Part2-Docker 的容器持久化各有一份单独的记录。
>
> 环境：Windows 11 本机 / nginx 1.30.5 官方 Windows 版（解压在 `C:\nginx-1.30.5`）/ 2026-10-13。**这一部分没有用虚拟机**——Part1/Part2 进虚拟机是因为 MySQL 和 Docker 本身，Part3 的 Practice 只涉及静态文件，而且 Part4 的 HTTP 要跟 05 那个 Java 后端打交道，环境留在 Windows 少一层网络绕行；唯一因环境而异的恰好就是 `root` 的写法，这本身就是本题的考点。
>
> 说明：文中报错、命令、输出都是**原文照抄**（error.log / access.log 逐行、`nginx -t` 与 `curl` 的原始输出）。**哪些是当场遇到的、哪些是收尾复核时才实测的，文中都单独标了**：改配置那一路的真实经过是问题 1~3；问题 4（相对路径到底相对谁）和问题 6（debug 日志）是我在整理这份记录时另起一个临时实例专门跑出来的验证，问题 5（多个实例并存）是复核时查到的机器状态、不是我当时踩到的坑——都不是当时的排错过程，标在各自的标题里。

---

## 一、题目要求（Part3 原文）

材料部分（「为什么需要 Nginx」那一整段）讲了四件事：单台服务器撑不住所以要加服务器 → 谁来管这些服务器（**反向代理**）→ 怎么分配压力（**负载均衡**）→ 静态资源为什么要交给它而不是后端程序；然后给了 `server` / `location` / `proxy_pass` 的配置骨架，和「`/api/messages` 命中 `/api/` 而不是 `/`，因为最长前缀优先」这条匹配规则，最后提了一句 `proxy_pass http://backend:8080` 里的 `backend` 是 docker-compose 的服务名。

**Practice 部分**：

> 现在需要你自己创建一个 HTML 文件到某位置并命名为 index.html，并写入给定的内容（一张居中卡片：🍍 图标 + `Pineapple` 标题，带渐变背景和 hover 上浮的 CSS）。
>
> 接着，需要你自己去补全 Nginx 的配置文件 nginx.conf，并通过 Nginx 来访问它。
>
> ```nginx
> server {
>     listen ____;
>
>     location / {
>         root ____;
>         index ____;
>     }
> }
> ```
>
> 请补全配置，使浏览器访问对应地址时能够显示网页。

**本题要求**：提交配置文件代码、运行截图。

---

## 二、整体思路的演变

| 阶段 | 我当时的想法 | 后来发现的问题 |
| --- | --- | --- |
| 1 | 材料写了反向代理、负载均衡、`upstream`、docker-compose 服务名一大堆，这题要写的东西应该不少 | **那一段是这一整个部署单元的手册，不是这道 Practice 的答案。** 四个空里 `listen 80;` 和 `index index.html index.htm;` 是官方默认 conf 里本来就写着的，真正要决定的只有 `root` 一行 |
| 2 | 改完 conf 就打开浏览器看一眼，能出页面就算过 | 「浏览器能打开」证明不了状态码。改成先 `nginx -t` 验配置、再 `curl -I` 看状态码和响应头 |
| 3 | `root` 就是告诉 nginx「我的文件放在哪个文件夹」 | `root` 是**进程视角**的路径，不是资源管理器里的路径。写错了它不会报「路径非法」，只会 404——而 error.log 会把 nginx 眼里的那个路径念出来 |
| 4 | 服务起不来和 404 差不多，都是「没弄好」 | 两类报错指向两层：**配置语法层**（`nginx -t` 报配置行号）和**文件系统层**（error.log 报拼出来的路径）。搞清这一点，排查方向就不用猜 |
| 5 | 改完 conf 执行 `nginx -s reload`，一定生效 | 机器上**同时跑着多个 nginx 实例**时，`reload` 只落到 `logs/nginx.pid` 里记的那一个 master，其余实例继续用旧配置服务 |
| 6 | 题目没说用不用虚拟机，那就随便 | 唯一因环境而异的就是 `root` 的写法（Windows 盘符正斜杠 vs 容器内绝对路径）。也就是说，**环境差异不是这道题的干扰项，它本身就是考点**，选哪个环境得能说出理由 |

**一句话总结思路转变**：从「把四个空填上、能看到菠萝页」→「**这四个空里，`root` 那一行指向的字符串，在 nginx 的进程眼里会长成什么样，我怎么证明它是对的**」。要填的东西只有一行，但那一行背后是「同一个目录，在宿主机上和在服务进程里是两个不同的东西」——这一句才是这道题真正要考的东西。

---

## 三、逐个问题记录

### 问题 1：四个空里其实只有一个决定（以及材料为什么写那么长）

**原文（官方默认 conf，与本题相关的部分）**

```nginx
server {
    listen       80;
    server_name  localhost;

    location / {
        root   html;
        index  index.html index.htm;
    }
    ...
}
```

**我当时以为的**：四个空要一个个填，材料里讲的 `proxy_pass`、`upstream`、负载均衡都得用上。

**实际的情况**：官方默认 conf 里 `listen 80;` 和 `index index.html index.htm;` **本来就写着**，两个空只需要抄下来。我真正改的只有一行：

```diff
-        root   html;
+        root   D:/java/2/07;
```

**原因**：这题的四个空不是四个知识点，而是「三个默认值 + 一个决定」。`listen` 回答「监听哪个端口」（Windows 上 80 空着，直接用它），`index` 回答「请求目录时拿哪个文件当响应」，这两条抄默认值就够；`root` 回答「这个站点的根目录在我的进程眼里叫什么」，**只有这一条是你自己的信息，也只有这一条会出错**。

至于材料里那大半篇 `proxy_pass` / `upstream` / 服务名——它是给后面的 HTTP 部分和 05 那个 Java 后端预备的（`com.test.Server` 真要挂到域名下面、后面再扩成多实例时，才轮到反向代理和负载均衡登场）。这道 Practice 只切了「静态资源」这一小片，用不上很正常，答卷里也不需要硬塞进去证明什么。

**收获**：觉得「学了一大堆、只改一行」的时候，要看的是**那一行背后的东西有没有搞懂**，而不是行数。材料的广度和题目的深度本来就不是一回事。

---

### 问题 2：`root` 是「进程视角」的路径，不是我文件夹里的路径

**证据原文（error.log 第 1~3 行，改之前两条、改之后一条）**

```
2026/10/13 08:58:40 [error] 32412#34564: *1 CreateFile() "C:\nginx-1.30.5/html/favicon.ico" failed (2: The system cannot find the file specified), client: 127.0.0.1, server: localhost, request: "GET /favicon.ico HTTP/1.1", host: "localhost", referrer: "http://localhost/"
2026/10/13 08:59:10 [error] 32412#34564: *3 CreateFile() "C:\nginx-1.30.5/html/favicon.ico" failed (2: The system cannot find the file specified), client: 127.0.0.1, server: localhost, request: "GET /favicon.ico HTTP/1.1", host: "127.0.0.1"
2026/10/13 09:04:03 [error] 39480#30588: *3 CreateFile() "D:/java/2/07/favicon.ico" failed (2: The system cannot find the file specified), client: 127.0.0.1, server: localhost, request: "GET /favicon.ico HTTP/1.1", host: "localhost"
```

**这行日志是怎么来的**：浏览器打开页面时会自己去要 `/favicon.ico`（第 1 行末尾的 `referrer: "http://localhost/"` 就是这么来的），而这个页面没放图标，所以每次打开都留一条 404。**这两条 404 不是配置错了**，但它意外地成了整份记录里最有价值的一条证据。

**怎么读它**——拿第 3 行逐字拆：

| 字段 | 是什么 |
| --- | --- |
| `2026/10/13 09:04:03` | 时间（access.log 里还带 `+0800` 时区，error.log 不带） |
| `[error]` | 日志级别。默认只记到 error，所以**每出现一行就意味着一次失败** |
| `39480#30588` | **进程号 # 线程号**（Linux 上这两个数通常相同，Windows 上不同）。39480 正是当时在 `:80` 上收请求的那个 worker——**有这个字段就能把每条日志归到具体哪个实例上** |
| `*3` | **连接号**。同一条 TCP 连接上的多条日志共用一个号，access.log 里没有这个字段 |
| `CreateFile()` | **nginx 实际调用的那个 Windows API**（Linux 上这里会写 `open()`）。说明配置解析、location 匹配、URI 处理全都过了，已经走到「向操作系统要文件」这一步 |
| `"D:/java/2/07/favicon.ico"` | **整行的价值都在这对引号里**：这是 nginx 亲手拼出来、递给操作系统的字符串 |
| `failed (2: ...)` | Win32 错误码。**2 = 文件不存在，3 = 路径（目录）不存在** |
| `client:` `server:` `request:` `host:` | 上下文 |

**路径是怎么拼出来的**

```
日志里的路径 =  root 求值后的结果  +  请求的 URI
```

- 第 3 行（`root D:/java/2/07;`）：绝对路径，直接拿来用 → `D:/java/2/07` + `/favicon.ico`
- 第 1、2 行（改之前是 `root html;`）：相对路径，先补全 → `C:\nginx-1.30.5` + `/html` + `/favicon.ico`

看第 1 行那串 `C:\nginx-1.30.5/html/favicon.ico`——**一条路径里两种斜杠并存**。那不是笔误，是「它由两个来源拼接」的指纹：`C:\nginx-1.30.5` 是 nginx 自己算出来的前缀（Windows API 返回反斜杠），`/html/favicon.ico` 来自我写的 root 和 URL 里的路径。

**原因**：`root` 的语义不是「我的文件放在哪」，而是「**在我的进程的文件系统视野里，网站根目录叫什么**」。日志里这串字符就是 nginx 眼里的世界；它和我脑子里的路径对不上，就是 404 的成因。

**解决 / 收获**：

1. 排查 404 不用去资源管理器里翻，**看这行日志，它会替你念出 nginx 眼里的正确写法**。
2. 同一件事放到容器里就是：`root` 要写 `/usr/share/nginx/html`。容器有自己的 mount namespace，宿主机路径在它眼里根本不存在——Part2 学的挂载，在这里换了个尺度重新出现。
3. Windows 下写 `D:/java/2/07`（正斜杠），日志原样回显，nginx 不做任何转换。
4. 顺带把面试常问的 `root` vs `alias` 也解释清楚了：**`root` 是前缀拼接，`alias` 是前缀替换**。上面这个「root 的值 + URI」的公式就是拼接。

---

### 问题 3：404 和 403 是两条不同的记录，来自不同的模块

**证据原文（error.log 第 6、7 行，以及对应的 access.log）**

```
2026/10/13 09:30:44 [error] 39480#30588: *13 CreateFile() "D:/java/2/07/does-not-exist.html" failed (2: The system cannot find the file specified), client: 127.0.0.1, server: localhost, request: "GET /does-not-exist.html HTTP/1.1", host: "localhost"
2026/10/13 09:30:44 [error] 39480#30588: *14 directory index of "D:/java/2/07/AI记录/" is forbidden, client: 127.0.0.1, server: localhost, request: "GET /AI%E8%AE%B0%E5%BD%95/ HTTP/1.1", host: "localhost"
```

```
127.0.0.1 - - [13/Oct/2026:09:04:03 +0800] "GET /favicon.ico HTTP/1.1" 404 153 "-" "curl/8.21.0"
127.0.0.1 - - [13/Oct/2026:09:48:26 +0800] "GET /AI%E8%AE%B0%E5%BD%95/ HTTP/1.1" 403 153 "-" "curl/8.21.0"
```

**现象**：同样是「没拿到页面」，一个是 404 一个是 403，error.log 里也是两句完全不同的记录。

**原因**：

- **404**：`CreateFile() ... failed` —— 静态文件模块走到 content 阶段，向操作系统要文件，**文件真的不存在**。
- **403**：`directory index of "..." is forbidden` —— 目录**在**，`index` 指令去找 `index.html` / `index.htm`，一个都没找到，而目录列表又是关着的，于是 index 模块拒绝这个请求。

失败的模块不一样，状态码就不一样。

**顺带发现的一件事**：403 那条日志里同时出现了两种表示——`request:` 字段是 URL 编码的 `%E8%AE%B0%E5%BD%95`（**URL 空间**），而路径里是解码后的中文 `AI记录`（**文件系统空间**）。中间那次 URL 解码正是 nginx 干的活，这也解释了同一行里两个字段为什么长得不一样。

**收获**：

| 现象 | 报错长什么样 | 该查哪一层 |
| --- | --- | --- |
| nginx 起不来 | `nginx: [emerg] ... in conf/nginx.conf:23` —— **报配置行号** | 配置语法层 |
| 起来了但 404 | error.log 里 **`CreateFile() "绝对路径" failed`** | 文件系统层 |
| 目录请求 403 | error.log 里 **`directory index of "绝对路径/" is forbidden`** | 文件系统层（但问题在缺索引文件） |

两类报错指向两层，看一眼就知道往哪查。这是这道题里我认为最值得带走的一条。

---

### 问题 4：相对 `root` 到底相对谁——前缀 = 启动时的当前目录（复核时实测）

**为什么会有这个问题**：第 1、2 行日志里，`root html;` 被补全成了 `C:\nginx-1.30.5/html`。我一开始猜是「相对 conf 文件所在的目录」，但 08:58 那两次请求是在 09:01 改 conf 之前发生的，光看日志分不清「相对 conf 目录」还是「相对启动目录」——这次恰好是同一个目录，两种解释都成立。

**实测原文**（换一个完全无关的工作目录，只做语法检查，不启动服务）

```
$ cd /tmp && "C:/nginx-1.30.5/nginx.exe" -t
nginx: [alert] could not open error log file: CreateFile() "logs/error.log" failed (3: The system cannot find the path specified)
2026/10/13 09:50:08 [emerg] 668#30224: CreateFile() "C:\Users\星语落弦\AppData\Local\Temp/conf/nginx.conf" failed (3: The system cannot find the path specified)
nginx: configuration file C:\Users\星语落弦\AppData\Local\Temp/conf/nginx.conf test failed
```

**结论**：前缀就是**启动时的当前目录**（不是 conf 所在目录，也不是 exe 所在目录）。`nginx -V` 里 `--prefix=` 是空的，所以前缀完全由启动目录决定。这次是 `/tmp` → `C:\Users\星语落弦\AppData\Local\Temp`。

**注意这里出现的错误码是 `3`**（路径不存在），和前面 favicon 的 `2`（文件不存在）刚好是两种不同的「找不到」——第一行那句 `could not open error log file` 就是「相对路径的日志文件也按同一个前缀找」的证据：`<启动目录>/logs/` 不存在，所以 error.log 都开不了。

**解决 / 收获**：这解释了为什么 nginx 的命令（`-t`、`-s reload`、`-s stop`）**都得在 `C:\nginx-1.30.5` 里执行**：`logs/`、`temp/`、`logs/nginx.pid` 这些相对路径全部按启动目录解析，在别处跑就会找不到 pid 文件，进而找不到那个要 reload 的 master。

---

### 问题 5：机器上同时跑着多个 nginx 实例，`reload` 只会落到一个（复核时发现的状态）

**证据原文**

```
nginx.exe  5660  启动于 9:06:11   ← 在 0.0.0.0:80 LISTENING
nginx.exe  13876 启动于 9:12:32   ← 在 0.0.0.0:80 LISTENING
nginx.exe  39480 启动于 9:03:56   ← 在 0.0.0.0:80 LISTENING
nginx.exe  11280 启动于 9:06:18
nginx.exe  34868 启动于 9:06:10
nginx.exe  9980  启动于 9:03:56

logs/nginx.pid = 11280
```

**现象**：`worker_processes 1` 的时候，一个实例只该有 master + worker 两个进程。这里 6 个进程、3 个 PID 在 `:80` 上抢着收请求，说明 nginx 被启动过不止一次；而 `logs/nginx.pid` 里只记着一个 pid。

**原因**：`nginx -s reload` 的工作方式是**读 `logs/nginx.pid` → 给那个 master 发信号**。它不是「把机器上所有 nginx 都重读一遍配置」。所以多个实例并存时，只有 pid 文件对应的那个实例换了新配置，其余实例继续用启动时那份旧配置服务——**请求落到哪个实例是随机的**，表现出来就是「我明明改了配置，怎么有时生效有时不生效」。

**如实说明**：这是我在收尾复核时查到的机器状态，**不是当时就踩到的坑**——我改配置那一路恰好改的就是最新那个实例，没出现「改了不生效」的现象。但它确实是一颗定时炸弹，尤其下一节要开 debug 日志、要反复 reload 的时候。

**解决 / 收获**：改配置之前先确认只有一个实例。清干净重来（在 cmd 或 PowerShell 里执行，别在 Git Bash 里跑——MSYS 会把 `/F`、`/IM` 这种斜杠参数当成路径改写）：

```bat
cd /d C:\nginx-1.30.5
taskkill /F /IM nginx.exe
tasklist | findstr nginx      :: 应该为空
nginx.exe                     :: 只起这一次
netstat -ano | findstr :80    :: 应该只剩一组
```

另外 `logs/nginx.pid` 这个文件是理解 nginx 进程管理的钥匙：它记的就是「当前该给谁发信号」。

---

### 问题 6：用 debug 日志把路径解析变成「现场直播」（复核时实测）

**背景**：前面所有证据都是**事后**的（先 404，再翻日志看它想要哪个路径）。`nginx -V` 的 configure 参数里有 **`--with-debug`**，也就是可以把日志级别开到 debug，让 nginx 把它**匹配 location、拼路径、探测文件**的每一步主动写出来。

**实测原文**（另起一个临时实例：`listen 8099`、`root D:/java/2/07`、`error_log debug`，发一个 404 请求。原文一次 404 记录了几十行，下面是节选，略去的行都标了出来）

```
2026/10/13 09:52:54 [debug] 37144#22696: *1 rewrite phase: 1
2026/10/13 09:52:54 [debug] 37144#22696: *1 using configuration ""
2026/10/13 09:52:54 [debug] 37144#22696: *1 http cl:-1 max:1048576
2026/10/13 09:52:54 [debug] 37144#22696: *1 post rewrite phase: 4
（—— 此处略去 generic phase: 5~7、access phase: 8~10、post access phase: 11、generic phase: 12~13 等十余行 ——）
2026/10/13 09:52:54 [debug] 37144#22696: *1 content phase: 14
（—— 此处略去 content phase: 15~18 ——）
2026/10/13 09:52:54 [debug] 37144#22696: *1 content phase: 19
2026/10/13 09:52:54 [debug] 37144#22696: *1 http filename: "D:/java/2/07/does-not-exist.html"
2026/10/13 09:52:54 [debug] 37144#22696: *1 add cleanup: 014D3608
2026/10/13 09:52:54 [error] 37144#22696: *1 CreateFile() "D:/java/2/07/does-not-exist.html" failed (2: The system cannot find the file specified), client: 127.0.0.1, server: , request: "GET /does-not-exist.html HTTP/1.1", host: "localhost:8099"
2026/10/13 09:52:54 [debug] 37144#22696: *1 http finalize request: 404, "/does-not-exist.html?" a:1, c:1
```

**怎么读**：

| 行 | 说明了什么 |
| --- | --- |
| `rewrite phase` / `access phase` / `content phase: 14 … 19` | nginx 处理一个请求要顺序走**若干个阶段**，静态文件模块坐在 content 阶段，index 模块在更前面拦目录请求——这就是问题 3 里 403 和 404 来自不同地方的机制层解释 |
| `using configuration ""` | 这次选中的是哪个 location。**空字符串**是因为临时配置把 root/index 直接写在 `server` 里、没写 location 块，隐式 location 没有名字；我本机的正式配置里写的是 `location /`，这一行会打印 `using configuration "/"` |
| `http filename: "D:/java/2/07/does-not-exist.html"` | **`root` 的本质最直白的表述**：nginx 主动告诉你它准备向操作系统要哪个字符串。比 error 那行更早、更干净 |
| `CreateFile() ... failed (2: ...)` | 拿了上面那个路径去要文件，失败 |
| `http finalize request: 404` | 整条请求以 404 收尾 |

**怎么开、怎么关**：

```nginx
http {
    error_log logs/debug.log debug;   # 加这一行（放 http 或 server 里都行）
    ...
}
```

```bat
nginx.exe -t
nginx.exe -s reload
:: 访问一次之后
:: 然后搜 debug.log：
::   findstr "http filename" logs\debug.log
```

**注意**：debug 日志**极其啰嗦**（我这次一个 404 就写了几十行），它会把每个连接、每个阶段都记下来。**验证完一定改回 `error` 级别并删掉 `debug.log`**，不能留在长期运行的配置里。另外这需要 `--with-debug` 编译的版本，官方 Windows 版带这个参数，`nginx -V` 里能查到。

**收获**：error.log 是尸检报告，debug 日志是现场直播。只有一次性的问题（不好复现、或者发生在启动阶段）才值得开 debug；平时靠 error.log 就够。**但答题时能拿出这样一份 trace，证明的不是「我配好了」，而是「我知道 nginx 每一步在干什么」**——这比截图里多十个浏览器页面都有说服力。

---

## 四、最终配置

`C:\nginx-1.30.5\conf\nginx.conf` 里与本题相关的部分（**相对于官方默认 conf，只改了一行 `root`**）：

```nginx
http {
    include       mime.types;
    default_type  application/octet-stream;

    sendfile        on;
    keepalive_timeout  65;

    server {
        listen       80;
        server_name  localhost;

        location / {
            root   D:/java/2/07;
            index  index.html index.htm;
        }

        error_page   500 502 503 504  /50x.html;
        location = /50x.html {
            root   html;
        }
    }
}
```

| 片段 | 含义 |
| --- | --- |
| `server { }` | 一个虚拟服务器（一个站点）。本题只有一个，所以没有 host 分流的问题 |
| `listen 80;` | 监听 80 端口。Windows 上这个端口空着，直接用它；浏览器访问 `http://localhost/` 默认就是 80，不用写端口号 |
| `location / { }` | 匹配所有以 `/` 开头的请求，也就是该站点下的全部请求。本题只有一个 location，没有匹配顺序问题 |
| `root D:/java/2/07;` | **本题唯一需要自己决定的一行**：这个站点的根目录，是 nginx **进程眼里**的路径。Windows 下用正斜杠 |
| `index index.html index.htm;` | 请求的路径指向一个目录时（比如 `http://localhost/`），按顺序找 `index.html`、`index.htm` 当响应；都找不到 → 403 |
| `location = /50x.html` | `=` 是精确匹配，只命中这一个路径；`error_page` 把 5xx 错误指到它。官方默认配置自带，本题没动 |

**两点说明**

1. 题目给的模板只有一个 `server { }` 块，但**真要能用，它得嵌在 `http { }` 里**——直接把它当成完整的 `nginx.conf` 是起不来的（`server` 不能出现在 `http` 外面）。我是在官方默认 conf 里原地改的，所以外面那层 `http { }`、`events { }` 本来就是齐的。如果重写一份，结构要按上面这样写全。
2. 我把 `root` 直接指到了作业目录 `D:/java/2/07`——**提交的那份 index.html 和运行的那份是同一个文件**，不用两边同步。副作用是**整个 07 目录都成了网站根目录**：实测 `http://localhost/微光2026招新-Web后端-07.md` 返回 200，也就是答卷正文、AI记录都变成了可以直接访问的静态文件。在 localhost 上做作业无所谓，但这正是「文档根目录不该随手指向工作目录」的原因，换到公网就是信息泄露。

---

## 五、运行与验证

**验证思路**：题目要的是「浏览器访问能显示页面」，但「浏览器能打开」这件事本身证明不了什么。所以按顺序验三样：**配置对不对（`nginx -t`）→ 响应对不对（`curl -I` 看状态码和 Content-Type）→ 路径解析对不对（error.log 里那串路径）**。

**（1）配置自检 `nginx -t`（在 `C:\nginx-1.30.5` 目录里执行）**

```
nginx: the configuration file C:/nginx-1.30.5/conf/nginx.conf syntax is ok
nginx: configuration file C:/nginx-1.30.5/conf/nginx.conf test is successful
```

两行分别是「语法没问题」和「整份配置可用」。**改完配置先过这一步再 reload**，能挡住绝大多数「改了没生效」的困惑。

**（2）响应头与状态码**

```
$ curl -I http://localhost/
HTTP/1.1 200 OK
Server: nginx/1.30.5
Date: Tue, 13 Oct 2026 01:30:34 GMT
Content-Type: text/html
Content-Length: 1275
Last-Modified: Tue, 13 Oct 2026 00:35:51 GMT
Connection: keep-alive
ETag: "6acd7ce7-4fb"
Accept-Ranges: bytes
```

| 行 | 证明了什么 |
| --- | --- |
| `HTTP/1.1 200 OK` | 页面正常，不是 404/403 |
| `Server: nginx/1.30.5` | **响应确实是 nginx 给的**，不是别的什么占着 80 端口 |
| `Content-Type: text/html` | nginx 按 `mime.types` 认出了扩展名，浏览器会当网页渲染而不是当文件下载 |
| `Content-Length: 1275` | 和 `07/index.html` 的实际字节数一致——**同一个文件**，没有拿错目录 |
| `Last-Modified` | 就是 index.html 的修改时间 |
| `Accept-Ranges: bytes` | 支持断点续传（`sendfile` 那一套的作用） |

**（3）四个请求，四种结果**

| 请求 | 状态码 | 走的是哪条路 |
| --- | --- | --- |
| `http://localhost/` | `200` | `location /` → `root` + `/` → 是目录 → `index` 找到 `index.html` |
| `http://localhost/does-not-exist.html` | `404` | 文件不存在，静态文件模块 `CreateFile()` 失败 |
| `http://localhost/AI记录/` | `403` | 目录在，但 `index.html` / `index.htm` 都没有 → index 模块拒绝 |
| `http://localhost/微光2026招新-Web后端-07.md` | `200` | `root` 指向哪，整个目录都是网站根目录 |

**（4）access.log（结果）与 error.log（原因）配对读**

```
127.0.0.1 - - [13/Oct/2026:09:12:38 +0800] "GET / HTTP/1.1" 200 1275 "-" "Mozilla/5.0 ... Edg/155.0.0.0"
127.0.0.1 - - [13/Oct/2026:09:48:26 +0800] "GET /AI%E8%AE%B0%E5%BD%95/ HTTP/1.1" 403 153 "-" "curl/8.21.0"
```

access.log 每行是 `客户端 - - [时间] "请求行" 状态码 响应字节数 "-" UA`：它回答**「发生了什么、结果如何」**（连 UA 是 Edge 还是 curl 都记着）。error.log 回答**「为什么」**。排错时两个对着看：access 里找那一行确认状态码，error 里找同一个连接号（`*13`、`*14`）看原因。

**结论**：`200` + `text/html` + 1275 字节，`root` 指向目录里的 `index.html`，题目要求的页面确实由 nginx 服务出来了。

---

## 六、概念小结

| 概念 | 一句话记住 |
| --- | --- |
| `server` / `location` / `root` / `index` | `server` 是一个站点，`location` 在这个站点里按路径分流，`root` 说明「进程眼里的站点根目录」，`index` 说明「请求目录时拿哪个文件当响应」 |
| `root` 的本质 | **不是「文件放在哪」，而是「在我的进程的文件系统视野里根目录叫什么」**。所以判断它对不对的标准是 error.log 里那串拼出来的路径，不是资源管理器里能不能打开 |
| root 的拼接公式 | `日志里的路径 = root 求值后 + 请求 URI`。`root /data` + 请求 `/a/x.html` → `/data/a/x.html`（**拼**）；换成 `alias /data/` → `/data/x.html`（**换**） |
| 相对路径的基准 | 相对 `root` 相对的是 **nginx 前缀目录 = 启动时的当前目录**（`--prefix=` 为空时），不是 conf 所在目录。所以 nginx 的命令必须在自己的安装目录里执行 |
| 错误码 2 与 3 | Win32：**2 = 文件不存在，3 = 路径（目录）不存在**。Linux 上是 `2: No such file or directory` / `3: No such file or directory`，但语义一样 |
| 404 vs 403 | **404 = 文件不存在**（静态文件模块，content 阶段 `CreateFile()` 失败）；**403 = 目录在但没有可用的索引文件**（index 模块拒绝）。失败的模块不同，状态码就不同 |
| error.log 的两个空间 | `request:` 字段是 **URL 空间**（URL 编码，`%E8%AE%B0%E5%BD%95`），路径部分已经是 **文件系统空间**（解码后的中文）。中间那次 URL 解码也是 nginx 干的 |
| access.log vs error.log | access 记**结果**（状态码、响应字节数、UA），error 记**原因**（失败的 API 调用、参数）。两个日志靠**连接号**（`*13`）和时间对应起来 |
| 日志里的 `pid#tid` | 进程号 # 线程号。多实例并存时，靠这个字段才能把每条日志归到具体实例 |
| 日志级别 | 默认只记到 `error`，所以**每有一行就意味着一次失败**。排查完记得把 level 改回去；debug 级别需要 `--with-debug` 编译的版本（官方 Windows 版带），且极其啰嗦 |
| `nginx -t` | 改完配置先跑它：「语法 ok」+「test is successful」两行都出现才算过。这是「改完必做」的第一步 |
| 分层排查法 | **起不来 → 报配置行号（配置层）；起来了但 404/403 → 报拼出来的路径（文件系统层）**。两类报错指向两层，看一眼就知道往哪查 |
| `nginx -s reload` 的范围 | 它只读 `logs/nginx.pid`、**给那一个 master 发信号**，不会去管机器上其它 nginx 实例。多实例并存 = 改了配置有时生效有时不生效 |
| 为什么用 Windows 做这题 | Part1/Part2 进虚拟机是因为 MySQL 和 Docker 本身；Practice 只涉及静态文件，跟容器无关。唯一因环境而异的是 `root` 的写法（Windows 盘符正斜杠 / 容器内绝对路径），而那恰好就是考点 |
| 材料的广度 ≠ 题目的深度 | 那大半篇反向代理、负载均衡、服务名解析，是给后面的 HTTP 部分和 Java 后端预备的。这一格只切了「静态资源」这一小片，用不上不代表白读 |
