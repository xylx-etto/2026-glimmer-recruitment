# 微光2026招新-Web后端-07 · Docker 环境与 MySQL 容器持久化 · 思考过程与问题记录

> 这一份记的是 **Part2-Docker** 这一部分：从在 Linux 里装 Docker，到把 MySQL 容器跑起来、把数据目录挂到宿主机 `~/mysql_data`、再 `rm -f` 删掉容器重建、验证数据还在的全过程——想过什么、错在哪、最后怎么改对的。
>
> Part1-MySQL 的书籍库设计（7-1 的 Task1~Task3）不在这份记录的范围里，那部分在答卷正文。
>
> 环境：VMware 上的 Ubuntu 虚拟机（`xylx-VMware-Virtual-Platform`），用 FinalShell 远程连接 / Docker CE + MySQL 8.0.46 / 2026-10
>
> 说明：文中所有报错、命令和输出都是**当时终端里的原文照抄**（排错过程中一条条贴出来的原文），没有事后凭印象改写；最后一次「删容器重建」的终端记录是从 FinalShell 里整段复制过来的。**哪些是真踩到的坑、哪些只是收尾时顺手记下的隐患，文中都单独标了。**

---

## 一、题目要求

**（1）装好 Docker 并跑一遍基础操作**

```bash
docker --version                      # 看看装好了没
docker run hello-world                # 看到 Hello from Docker! 就算通了
docker run -d -p 8080:80 nginx:alpine # 浏览器打开 http://localhost:8080 能看到 nginx 欢迎页
docker ps                             # 看运行中的容器
docker stop <容器ID>                   # 停掉它，再 docker ps -a 看所有容器状态
```

并用**自己的话**解释：镜像和容器到底有什么区别。

**（2）项目题：用 Docker 给用户系统搭一个标准化的测试库**（原文要点）

1. **获取与部署**：拉取 `mysql:8.0` 镜像，以后台模式运行容器；root 密码设为 `123456`；创建一个名为 `user_db` 的默认数据库；把宿主机的 `3306` 端口映射出来供本地开发工具连接。
2. **数据持久化**：将 MySQL 容器内的数据目录（`/var/lib/mysql`）挂载到宿主机的 `~/mysql_data` 目录，确保容器销毁后数据依然完好。
3. **连通验证与落库**：进入容器内部调用 `mysql` 命令行客户端，在 `user_db` 下创建一张 `users` 表并插入一条测试数据。
4. **删除验证**：直接使用 `docker rm -f` 强行删掉该容器，随后**使用一模一样的挂载参数**拉起一个新的 MySQL 容器，验证刚才插入的数据是否依然存在。

**（3）本题要求**：提交上述操作的运行截图并总结；并总结「你在哪个环节出了最多的问题，如果你是出题人，你会怎么去考察学习者对 docker 的掌握程度」。

---

## 二、整体思路的演变

| 阶段 | 我当时的想法 | 后来发现的问题 |
| --- | --- | --- |
| 1 | 「装上 Docker 就完了，`apt install` 一条命令的事」 | 光是把源配好就花了半小时：GPG 公钥缺失、源文件重复配置、国内网络拉不动 |
| 2 | 公钥报错，照第一种办法 `apt-key adv --recv-keys` 加 | 这条路已经**废弃**（未来会移除），而且它只把钥匙塞进全局信任，没有把钥匙和源绑定；正确做法是 `signed-by` / `.sources` 里写明密钥路径 |
| 3 | 「拉不下来 = 网络不好」 | 两次都判错了：一次是**连不上 registry**（要配镜像加速器），一次是**daemon 被上一个 pull 堵死**（`docker images` 也卡），后者跟网络没关系 |
| 4 | `-v mysql_data:/var/lib/mysql`，两边都当成「我虚拟机上的目录」 | 冒号**右边是容器里的路径**，宿主机上根本不该有 `/var/lib/mysql`——这一条卡了我最久 |
| 5 | 照教程写 `--name` + `--restart unless-stopped`，以为自动重启「会吃内存」 | 重启策略本身不占内存（只是一条配置记录）；但作业确实不需要，去掉了 |
| 6 | 用命名卷就够了 | 题目要的是挂到**宿主机 `~/mysql_data` 目录**，这是绑定挂载（bind mount），不是命名卷；写 `~` 还要注意权限 |
| 7 | 多行命令照着教程加 `\` 换行 | `\` **前面必须有空格**，否则换行被吃掉，整条命令粘成一坨，报的错却像是「多打了一个横杠」 |

**一句话总结思路转变**：从「把教程里那条 `docker run` 抄下来跑通」→「**`-p` 的左右是谁的、`-v` 的左右是谁的、`-e` 的名字是谁定的、报错该先看哪儿——每一步都得能说出个所以然**」。抄命令能跑通一次，但环境一变（国内网络、端口被占、镜像源挂了）就全废；这一路踩的坑其实都在这句话里。

---

## 三、逐个问题记录

### 问题 1：装 Docker 的第一道坎——`NO_PUBKEY`

**报错原文**

```
由于没有公钥，无法验证下列签名： NO_PUBKEY 7EA0A9C3F273FCD8
正在读取软件包列表... 完成
W: GPG 错误：https://download.docker.com/linux/ubuntu noble InRelease: 由于没有公钥，无法验证下列签名： NO_PUBKEY 7EA0A9C3F273FCD8
E: 仓库 “https://download.docker.com/linux/ubuntu noble InRelease” 没有数字签名。
N: 无法安全地用该源进行更新，所以默认禁用该源。
N: 参见 apt-secure(8) 手册以了解仓库创建和用户配置方面的细节。
```

**原因**

APT 要装 Docker 官方的包，就得先确认「这个源确实是 Docker 官方发的」。签名验证靠 GPG 公钥，而这台虚拟机里没有 Docker 的公钥，于是 `apt` 直接把整个源禁用掉——注意最后那句 `所以默认禁用该源`，意思是**这一步不修，后面所有 docker 的包都装不了**，不是「警告而已」。

**解决**

先试的是老办法（**这条后来被否掉了**）：

```bash
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 7EA0A9C3F273FCD8
```

`apt-key` 在新版 Ubuntu 里已经是 **deprecated**，而且它是把钥匙加到「全局信任」，风险更大。换成现在推荐的写法：把公钥单独存成一个文件，再在源配置里指明用哪个钥匙。

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

**收获**：`NO_PUBKEY` 不是「网络错误」，是**信任链缺一环**。看到 `E:` 开头的行要注意——`W:` 是警告，`E:` 是这条源真的被停用了。

---

### 问题 2：修完公钥还是不行——`sources.list.d` 里两份配置打架

**报错原文**（原文一共 12 条 `W:`，覆盖 Packages / Translations / DEP-11 / CNF，这里摘前两条）

```
W: 目标 Packages (stable/binary-amd64/Packages) 在 /etc/apt/sources.list.d/docker.list:1 和 /etc/apt/sources.list.d/docker.sources:1 中被配置了多次
W: 目标 Translations (stable/i18n/Translation-en) 在 /etc/apt/sources.list.d/docker.list:1 和 /etc/apt/sources.list.d/docker.sources:1 中被配置了多次
```

**原因**

`/etc/apt/sources.list.d/` 下同时存在 `docker.list`（旧的一行式格式）和 `docker.sources`（新的 deb822 格式），两份都指向同一个 Docker 仓库。**目录里每个文件都算一份源**，同一份内容出现两次，APT 就逐项报「被配置了多次」；而签名也还是没验过。

**我当时的原话**

> 装了半个小时还没装好

**解决**

半小时基本是卡在从 `download.docker.com` 拉公钥上。既然源本身也不干净，索性整段推倒，换成国内的**阿里云 Docker 源**（一次粘贴执行）：

```bash
# 1. 删除所有旧的 Docker 源文件，避免冲突
sudo rm -f /etc/apt/sources.list.d/docker.list /etc/apt/sources.list.d/docker.sources

# 2. 安装必要工具并创建密钥目录
sudo apt update
sudo apt install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings

# 3. 下载阿里云 Docker GPG 密钥（国内快）
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# 4. 添加阿里云 Docker 源
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list

# 5. 更新源
sudo apt update
```

这条不再报 `NO_PUBKEY` 也不是重复配置，之后才装上：

```bash
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

**收获**：**同一件事有两份配置，本身就是故障源。** `sources.list.d` 是「目录里每个文件都算一份」，重装/换源时要先 `ls -la /etc/apt/sources.list.d/ | grep docker` 看清楚有几份，别以为改了其中一个就完事。

---

### 问题 3：`permission denied`——普通用户碰不到 docker.sock

**报错原文**

```
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

**原因**

`/var/run/docker.sock` 默认属于 `root:docker`、权限 `660`。Docker 装好了，但当前用户不在 `docker` 组里，连不上这个 socket——**Docker 本身是好的，坏的是「我有没有资格跟它说话」**。

**解决**

```bash
sudo usermod -aG docker $USER
newgrp docker      # 组身份立刻在当个终端生效；否则要注销重登
docker ps
```

改完 `docker ps` 能列出容器了就通了。**顺带记住一条**：加进 `docker` 组等于给了这个用户接近 root 的权限（能通过挂载看到宿主机文件系统），个人虚拟机无所谓，生产机器要谨慎。

**收获**：「命令报权限错」有两种，要分清是**文件的权限**还是**socket 的权限**。这次报错里明确写了 `unix:///var/run/docker.sock`，等于直接把答案说出来了——**报错文本里带着路径，就先去看那个路径的属主和权限**。

---

### 问题 4：`connection refused`——拉不到 hello-world

**报错原文**

```
docker: Error response from daemon: failed to resolve reference "docker.io/library/helloworld:latest": failed to do request: Head "https://registry-1.docker.io/v2/library/helloworld/manifests/latest": dial tcp 162.125.1.8:443: connect: connection refused
```

**原因**

连不上 Docker Hub 的 `registry-1.docker.io`，国内网络下的常见情况。要配**镜像加速器**，让拉取走国内节点。

（这条报错里其实还藏着第二个问题：镜像名被写成了 `helloworld`，少了中间那个连字符。因为 `connection refused` 让整个请求根本没发出去，**这个名字错误当时没暴露出来**——后来成功的那次输出里，镜像名已经是 `hello-world` 了。）

**解决**

写 `/etc/docker/daemon.json`：

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.1ms.run",
    "https://docker.m.daocloud.io",
    "https://docker.xuanyuan.me"
  ]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

`docker info` 里能看到 `Registry Mirrors` 字段，说明生效了。然后：

```
$ docker run hello-world
Unable to find image 'hello-world:latest' locally
latest: Pulling from library/hello-world
4f55086f7dd0: Pull complete
d5e71e642bf5: Download complete
Digest: sha256:5e23090353324d887c48ad5e5c56d294eab81588df9605b07d1afe895f9cc8f8
Status: Downloaded newer image for hello-world:latest

Hello from Docker!
```

**收获**：**镜像加速器不是可选项，是国内装 Docker 的必做步骤**。另外一条教训是：一条报错只说明「它在哪一步停的」，不说明「这条命令写得对不对」——名字里的拼写错误被前面的网络错误挡住了。

---

### 问题 5：`port is already allocated`——查了半天，真凶是我自己的容器

**报错原文**

```
$ docker run -d -p 8080:80 nginx:alpine
06fa9bfcbdf76581cf736056d2e5a1cc186520e9af9160f744d0f71eef6af633
docker: Error response from daemon: failed to set up container networking: driver failed programming external connectivity on endpoint inspiring_sutherland (ffd4e257a7371dafe024ae1d5658d1e6a26cd9e506af73814eb210a363a69781): Bind for 0.0.0.0:8080 failed: port is already allocated
```

注意：容器 ID 已经打印出来了，**容器是被创建出来之后才在「配网络」这一步失败的**，所以还会留下一个跑不起来的空壳。

**我当时的判断**（错了一半）：以为 8080 被虚拟机里别的软件占了。查占用：

```
$ sudo lsof -i :8080
COMMAND     PID USER   FD   TYPE DEVICE SIZE/OFF NODE NAME
docker-pr 11717 root    8u  IPv4  79482      0t0  TCP *:http-alt (LISTEN)
docker-pr 11723 root    8u  IPv6  79483      0t0  TCP *:http-alt (LISTEN)
```

看到的是 `docker-pr`（`docker-proxy`）——**这不是「别的软件」，这正是 Docker 自己替某个容器在转发端口**。也就是说：占着 8080 的不是外人，是另一个容器。

**再查一层**

```
$ docker ps
CONTAINER ID   IMAGE          COMMAND                   CREATED         STATUS         PORTS                                     NAMES
ff432ef7aea4   nginx:alpine   "/docker-entrypoint.…"   4 minutes ago   Up 4 minutes   0.0.0.0:8080->80/tcp, [::]:8080->80/tcp   focused_agnesi
```

**原因**

`focused_agnesi` 就是之前那条 `docker run -d -p 8080:80 nginx:alpine` 跑起来的容器，它已经在用 8080 了，而且用的**就是同一个 `nginx:alpine` 镜像**。

**解决 / 结论**

我想做的事其实**已经做成了**，直接访问 `http://localhost:8080` 就是 nginx 欢迎页；刚才那次失败只是想把同一件事再做一遍。要么用这个容器，要么先 `docker stop focused_agnesi && docker rm focused_agnesi` 再重跑。

**收获**：**`lsof` 只看得到「谁占着端口」，看不到「哪个容器占着端口」**——端口转发是 `docker-proxy` 这个进程干的，要落到容器上必须再跑一次 `docker ps`（或 `docker ps --filter "publish=8080"`）。两个命令看到的层级不一样，缺哪个都定位不到。

---

### 问题 6：`docker images` 也卡住了——不是网络，是 daemon 被堵死

**现象**

```
8.0: Pulling from library/mysql      ← 卡着不动
```

换到另一个终端想看看进度：

> docker images 也卡住了

**原因**

`docker pull mysql:8.0` 还没结束就另开终端跑 `docker images`，两条命令**排队等同一个 daemon**，daemon 又被那个 pull 卡着 → 一起卡住。**这时候网络已经不是主要嫌疑了**：如果是拉取慢，`docker images` 这种纯本地查询不该也卡。

**解决**

```bash
sudo systemctl restart docker
```

重启后 daemon 解锁，pull 又能继续了（Docker 支持断点续传，已拉好的层不会重下）。当时也问过「能不能重启虚拟机」——可以，但那是最后手段：重启 Docker 服务最轻，`sudo reboot` 会断开 FinalShell，虚拟机管理界面强制重启可能丢数据。

**收获**：**判断「卡」的层次**——单个命令卡，可能是网络；**连本地查询命令都卡，那是 daemon 出问题了**。另外，`pull` 大镜像的时候别开多个终端同时敲 docker 命令，daemon 是单点的。

---

### 问题 7：`short read ... unexpected EOF`——镜像层没下完整

**报错原文**

```
short read: expected 316 bytes but got 0: unexpected EOF
```

**原因**

镜像层在下载/写入过程中不完整（网络抖动，或者上一次 daemon 卡死留下的残片）。

**解决**

按代价从小到大试：**重试 → 清理缓存并重启 → 换镜像源**。

```bash
docker pull mysql:8.0                       # 先重试，Docker 会重下损坏的层
docker system prune -a                      # 还不行，清掉未使用的镜像/容器/缓存
sudo systemctl restart docker
```

**怎么确认真的拉完整了**——最省事的办法是**再 pull 一次**：输出 `Image is up to date for mysql:8.0` 就说明本地已完整；还报 `short read` 就是没拉完。辅助证据：`docker images` 里有 mysql，`docker images --digests` 有 `sha256:` 摘要。

**收获**：`Digest: sha256:...` 这一行是「拉完整了」的凭证；只有层、没有 digest，说明还差最后一步。

---

### 问题 8：`Exited (1)`——容器一起来就死，第一现场是 `docker logs`

**现象**

```
Exited (1) 26 seconds ago             hungry_archimedes
```

**我当时的思路**（走对了方向）：只知道它退出了，不知道为什么 → 看日志。

**报错原文（`docker logs hungry_archimedes`）**

```
You need to specify one of the following as an environment variable:
    - MYSQL_ROOT_PASSWORD
    - MYSQL_ALLOW_EMPTY_PASSWORD
    - MYSQL_RANDOM_ROOT_PASSWORD
```

**原因**

MySQL 官方镜像的入口脚本要求**三个变量至少给一个**，否则直接 `exit 1`。启动命令里少了 `-e MYSQL_ROOT_PASSWORD=...`。

**解决**

删掉这个空壳容器，重新跑，把密码带上（同一条命令里顺便补了 `--name` 和 `-v`）：

```bash
docker rm hungry_archimedes
docker run -d --name mysql8 -e MYSQL_ROOT_PASSWORD=123456 -p 3306:3306 -v mysql_data:/var/lib/mysql mysql:8.0
```

**收获**：**容器启动失败，第一个该敲的是 `docker logs 容器名`**，不是重新 `run` 一遍。这次日志把原因一字不差地写出来了——`Exited (1)` 这种「只有结论没有原因」的输出，原因一定在日志里。

---

### 问题 9：`unknown shorthand flag: '-' in ---name`——报错说的是 A，错其实在 B

**报错原文**

```
unknown shorthand flag: '-' in ---name
```

**我当时的反应**

> 我没打错啊

我把实际敲的贴出来了（注意行首的 `>` 是 shell 的续行提示符）：

```
docker run -d\
> --name user_db\
> -e MYSQL_ROOT_PASSWORD=123456\
> -p 3306:3306\
> -v ~/mysql_data:var/lib/mysql\
> mysql:8.0
```

**原因（两处，都不在报错指的那个位置）**

**① `\` 前面没有空格。** `\` 紧贴着 `-d`，shell 把换行符吃掉之后拼出来的是：

```
docker run -d--name user_db-e MYSQL_ROOT_PASSWORD=...
```

`-d` 和 `--name` 粘成了 `-d--name`，Docker 解析到这一串就报 `unknown shorthand flag: '-' in ---name`——**它说的是「`--` 前面那个 `-d` 后面跟了怪东西」，看起来像「多打了一个横杠」**。正确写法是每个 `\` **前面有一个空格、后面直接换行**：

```bash
docker run -d \
  --name user_db \
  ...
```

**② `-v ~/mysql_data:var/lib/mysql` 冒号右边少了一个开头的 `/`。** 应该是 `/var/lib/mysql`，少了斜杠会被当成相对路径。

**解决**

```bash
docker run -d --name user_db -e MYSQL_ROOT_PASSWORD=123456 -p 3306:3306 -v ~/mysql_data:/var/lib/mysql mysql:8.0
```

最后干脆写成**一行**跑通——多行命令的 `\` 是新手最容易翻车的地方，一行版复制粘贴不会因为换行出问题。

**收获**：**报错里的「位置」和「原因」可以隔着好几层**。这条报错字面上在说横杠个数，真凶是上一行的续行符。另外记牢规则：**一个 `-` 是短选项（`-d`/`-p`/`-v`），两个 `--` 是长选项（`--name`/`--restart`），没有三个横杠的写法**；`\` 是「这行没写完」，不是命令的一部分。

---

### 问题 10：在宿主机上怎么都找不到 `/var/lib/mysql`——左右两边的认知断层（卡最久）

**我当时的原话**

> 我的根目录或者mysql_data里都没有/var/lib/mysql这个

**原因**

`-v ~/mysql_data:/var/lib/mysql` 里，**冒号两边是两个不同世界的东西**：

| 位置 | 属于谁 | 要不要我自己建 |
| --- | --- | --- |
| `~/mysql_data`（左） | **宿主机**（我的虚拟机） | 要，`mkdir -p ~/mysql_data` |
| `/var/lib/mysql`（右） | **容器内部**（MySQL 镜像里自带的） | 不要，宿主机上也**不应该**有 |

所以「宿主机上没有 `/var/lib/mysql`」是**正常的**，而不是我没建对。这一条卡我最久——**我把左右两边都当成了宿主机上的目录，于是怎么找都找不到，还以为是自己少建了一个目录。**

**解决**

宿主机上只做一件事：

```bash
mkdir -p ~/mysql_data
chmod 777 ~/mysql_data     # MySQL 容器里的 mysql 用户 UID 通常是 999，权限不对会启动失败
```

然后跑容器，用「两边内容一致」来验证挂载：

```bash
ls ~/mysql_data            # 出现 mysql/ performance_schema/ sys/ 和 ibdata1 之类的文件，就是挂上了
docker exec -it user_db ls /var/lib/mysql    # 容器里看到的，是同一份
```

**收获**：**`-v 左:右` 的口诀——左宿主机、右容器，容器里那个路径镜像早就有了，我只需要准备左边。** 数据实际落在左边（宿主机），所以删容器不丢。

---

### 问题 11（附带）：FinalShell 连不上——这次排错中途的插曲

**报错原文**

```
java.net.ConnectException: Connection refused: connect
```

后来变成一直卡在「连接主机…」。

**当时的方向**：`refused` 和 `timed out` 是两回事——`refused` 说明机器能通、只是 22 端口上没有服务在听（或者被防火墙拒了）；一直卡住则更像能连上但握手没完成。查的顺序是：虚拟机能登录吗 → `ip a` 看 IP 变没变 → `sudo systemctl status ssh` 看服务 → `sudo ss -tlnp | grep :22` 看端口 → `ufw status` / `iptables -L` 看防火墙（Docker 会改 iptables，反复重启 daemon 之后很容易怀疑到它头上）。

实际原因是**在 VMware 里恢复快照**：恢复之后虚拟机的状态整个回滚了，SSH 也跟着没起来。**这一条和本作业没关系**，但它占掉了我中间一段时间的注意力——记在这里是想说明：**排错时先分清「我改动过什么」**。那次我一直在折腾 Docker，于是第一反应全往 Docker/iptables 上猜，其实是另一件事（快照）导致的。

**收获**：连着折腾完 A 之后遇到 B，人很容易把 B 也归因给 A。**先看 B 自己的状态（服务在不在、端口在不在听），比顺着「可能是刚才动的东西影响的」往下猜快得多。**

---

## 四、最终命令

宿主机侧先准备挂载点：

```bash
mkdir -p ~/mysql_data
chmod 777 ~/mysql_data
```

启动容器（题目要求的密码 `123456`、`3306` 映射、挂载 `~/mysql_data`）：

```bash
docker run -d \
  --name user_db \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -p 3306:3306 \
  -v ~/mysql_data:/var/lib/mysql \
  mysql:8.0
```

逐行的意思：

| 片段 | 含义 |
| --- | --- |
| `-d` | 后台运行，不占着终端 |
| `--name user_db` | 给容器起名（不起名会随机得到 `hungry_archimedes` 这种名字，后面根本记不住） |
| `-e MYSQL_ROOT_PASSWORD=123456` | 传给 MySQL 镜像的环境变量，**变量名是镜像定的，不是 Docker 的** |
| `-p 3306:3306` | 左＝宿主机端口，右＝容器端口 |
| `-v ~/mysql_data:/var/lib/mysql` | 左＝宿主机目录，右＝容器内数据目录 |
| `mysql:8.0` | 镜像:标签 |

进 MySQL 客户端落库（**进的是 MySQL 客户端，不是容器的 Linux shell**）：

```bash
docker exec -it user_db mysql -uroot -p
# 回车后输密码 123456（输入时不显示），看到 mysql> 提示符即成功
```

```sql
CREATE TABLE users (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_name VARCHAR(50)
);
INSERT INTO users (user_name) VALUES ('xylx');
```

建表那条语句没有留在终端记录里，上面是照 `select *` 的表头（`id`、`user_name` 两列）和插入的行反推的。

**关于 `--restart unless-stopped`**：教程模板里有，我最后**没加**。它本身不占内存（只是容器配置里的一条策略记录），但不加更符合作业场景——手动停掉之后它就真的不再自己冒出来，行为更好预期。长期跑的服务才需要它。

---

## 五、运行与验证

**验证思路**：题目要证的是「**容器销毁后数据依然完好**」。所以不能只看「容器在跑」，必须走完整条链路——**建数据 → 强行删容器 → 用一模一样的挂载参数重建 → 再查一次数据**。

**终端原文（从 FinalShell 整段复制）**

```
xylx@xylx-VMware-Virtual-Platform:~$ docker rm -f 
focused_agnesi        lucid_swartz          user_db               
inspiring_sutherland  magical_hugle         vibrant_hamilton      
xylx@xylx-VMware-Virtual-Platform:~$ docker rm -f user_db 
user_db
xylx@xylx-VMware-Virtual-Platform:~$ docker run -d --name user_db -e MYSQL_ROOT_PASSWORD=123456 -p 3306:3306 -v ~/mysql_data:/var/lib/mysql mysql:8.0
8d00d1f5bd7b5331fce563cadebd1305b89a89d15a1ad54af38fc28b2221b14f
xylx@xylx-VMware-Virtual-Platform:~$ docker exec -it user_db mysql -uroot -p123456
mysql: [Warning] Using a password on the command line interface can be insecure.
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 8
Server version: 8.0.46 MySQL Community Server - GPL

Copyright (c) 2000, 2026, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> use users;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

Database changed
mysql> select * from users;
+------+-----------+
| id   | user_name |
+------+-----------+
|    1 | xylx      |
+------+-----------+
1 row in set (0.00 sec)
```

**怎么读这段输出**

| 行 | 证明了什么 |
| --- | --- |
| `docker rm -f user_db` → `user_db` | 旧容器被**强制删除**了（不是 stop，是删掉） |
| `docker run` 打印出新的容器 ID `8d00d1f5...` | 重建的是**一个全新的容器**，挂载参数和之前完全相同 |
| `use users;` → `Database changed` | 库还在（如果数据没了，这里会报 `Unknown database`） |
| `select * from users;` → `1 row in set`，id=1 / user_name=xylx | **之前插进去的那条数据还在** |

**结论**：容器被删掉、重建之后数据仍在，说明数据不是存在容器里的，而是**通过 `-v ~/mysql_data:/var/lib/mysql` 存到了宿主机的 `~/mysql_data`**——这正是题目要验证的点。

**两点如实说明（都是收尾复核时发现的，不是当时就踩了的坑）**

1. 上面的启动命令里**没有** `-e MYSQL_DATABASE=user_db`，而题目要求「创建一个名为 `user_db` 的默认数据库」；终端记录里出现的库名是 `users`（容器名才叫 `user_db`）。两者对不上，这一点我记在这里，没有把它写成「当时就是这么对的」。
2. 那次 `docker rm -f` 是**连着删了两个容器**（先空跑一次列出名字，再 `docker rm -f user_db`）。另外，之前端口冲突留下的 `inspiring_sutherland`、`focused_agnesi` 也都在 `docker ps -a` 列表里——**失败的 `docker run` 会留下 `Created` 状态的空壳容器**，事后要清。

---

## 六、概念小结

| 概念 | 一句话记住 |
| --- | --- |
| Docker 仓库 vs 镜像站 | **仓库（Registry）**才是真正存镜像、能 push/pull、有权限管理的地方（Docker Hub、Harbor、`registry:5000`）；**镜像站**是把仓库内容缓存/代理到就近节点做加速的（如 `docker.mirrors.ustc.edu.cn`、阿里云 docker-ce 源）。镜像站通常只读、不能推。`daemon.json` 里的 `registry-mirrors` 填的是**镜像站** |
| 镜像 vs 容器 | 镜像是只读模板（类），容器是它跑起来的实例（对象）；同一个镜像能起多个互不干扰的容器 |
| `-p 3306:3306` | **左＝宿主机端口，右＝容器端口**。右边由容器里服务监听什么决定（MySQL 固定 3306），左边随便改：宿主机 3306 被占了就写 `-p 3307:3306` |
| `-v 左:右` | **左＝宿主机目录（要自己建），右＝容器内目录（镜像自带的，宿主机上没有）**。删容器不丢数据，是因为数据落在左边 |
| 命名卷 vs 绑定挂载 | `mysql_data:/var/lib/mysql` 是**命名卷**（Docker 管，存在 `/var/lib/docker/volumes/` 下）；`~/mysql_data:/var/lib/mysql` 是**绑定挂载**（我自己指定的宿主机目录）。本题要后者 |
| `-e` 与变量名 | **`-e` 是 Docker 的参数，变量名是镜像定的**。`MYSQL_ROOT_PASSWORD` 是 MySQL 镜像 `docker-entrypoint.sh` 里读的约定，看 Docker Hub 页面或 `docker exec 容器 env` 才能知道。且**只在数据目录为空、首次初始化时生效**——卷里已有数据，再改这个变量不会改密码 |
| `docker logs` | 容器启动失败（`Exited (1)`）的第一现场。90% 的原因日志里写清楚了 |
| `--name` | 不起名会得到随机名字（`hungry_archimedes`），后面 `exec`/`stop` 全靠它，必须起 |
| `\` 续行符 | **前面必须有空格、后面直接换行**。少了空格会把换行吃掉，命令粘成一坨，报错却指向别处 |
| `-` 与 `--` | 一个横杠是短选项（`-d`/`-p`/`-v`/`-it`），两个是长选项（`--name`/`--restart`），没有三个 |
| `docker exec` 两个层次 | `docker exec -it 容器 bash` 进的是**容器的 Linux shell**；`docker exec -it 容器 mysql -uroot -p` 进的是**MySQL 客户端**。要建表就进后者 |
| 分号 | MySQL 客户端里 `;` 是**语句结束符**，没打分号提示符会变成 `->` 继续等；Linux shell 里的 `;` 是命令分隔符，两回事 |
| daemon 是单点的 | `docker pull` 大镜像时别另开终端敲 `docker` 命令；**连 `docker images` 都卡**＝daemon 被堵死，`sudo systemctl restart docker` |
| 容器的退出码 | `Exited (1)` 只是结论；配合 `docker ps -a` 看状态、`docker logs` 看原因 |
