# BiliBili Dynamic (OneBot 11)

一个可低延迟检测 B 站动态/直播并转发到 QQ 群的独立应用，通过 [OneBot 11](https://github.com/botuniverse/onebot-11) 协议对接 QQ，完全基于 [bilibili-dynamic-mirai-plugin](https://github.com/Colter23/bilibili-dynamic-mirai-plugin) 通过 Claude Opus 4.6 改造而来

![Stars](https://img.shields.io/github/stars//PokersKun/bilibili-dynamic-onebot)
![Downloads](https://img.shields.io/github/downloads//PokersKun/bilibili-dynamic-onebot/total)
[![Release](https://img.shields.io/github/v/release//PokersKun/bilibili-dynamic-onebot)](https://github.com//PokersKun/bilibili-dynamic-onebot/releases)

## 特性

**无论多少订阅均可在最低 10s 内检测所有动态**
使用 [skiko](https://github.com/JetBrains/skiko) 绘图
番剧订阅
动态过滤
扫码登录
可配置性高

## 样式预览

<img src="docs/img/demo1.png" width="400" alt="样式1">
<img src="docs/img/demo2.png" width="400" alt="样式2">
<img src="docs/img/demo3.png" width="400" alt="样式3">

## 反馈

在 [bilibili-dynamic-mirai-plugin](https://github.com/Colter23/bilibili-dynamic-mirai-plugin) 提供的 QQ 群里反馈 [734922374](https://jq.qq.com/?_wv=1027&k=NuSQdKTQ)

## 安装

### 环境要求

- Java 11 或更高版本
- 一个支持 OneBot 11 正向 WebSocket 的 QQ Bot 实现（如 [NapCat](https://github.com/NapNeko/NapCatQQ)、[Lagrange](https://github.com/LagrangeDev/Lagrange.Core) 等）
- 配置 WebSocket 服务器时需要选择 Array 数据格式，Token 可填可不填

### 下载

从 [Releases](https://github.com//PokersKun/bilibili-dynamic-onebot/releases) 下载最新的 JAR 文件

### 运行

```bash
java -jar bilibili-dynamic-onebot.jar [数据目录] [配置目录]
```

- `数据目录`：数据存储路径，默认 `./data`
- `配置目录`：配置文件路径，默认 `./config`

首次运行会自动生成默认配置文件，请先编辑 `config/OneBotConfig.yml` 配置 WebSocket 地址和管理员账号。

### 配置 OneBot 连接

编辑 `config/OneBotConfig.yml`：

```yaml
# OneBot WebSocket 正向连接地址
wsUrl: ws://127.0.0.1:3001
# Bot 管理员 QQ 号列表（只有管理员才能使用所有命令，可配置多个）
adminIds:
  - 123456789
# 连接 OneBot 时使用的 Token（可选，留空则不使用）
token: ''
```

## 指令

> **所有命令仅限管理员使用**（在 `OneBotConfig.yml` 的 `adminIds` 中配置）

常用指令帮助图可通过 `/bili help` 获取
发送 `/bili` 可查看所有可用命令

### 基础指令

| 订阅相关 | 描述 |
|---------|------|
| `/bili help` (h) | 获取帮助图片 |
| `/bili login` | 扫码登录B站 |
| `/bili add` (follow) `<UID> [目标]` | 添加订阅 (支持番剧ID: `ss11111` `md22222` `ep33333`) |
| `/bili del` (unfollow) `<用户> [目标]` | 取消订阅 (支持用户名模糊匹配) |
| `/bili delall [目标]` | 删除全部订阅 |
| `/bili clear` | 清理无效群/好友订阅 |

| 订阅列表 | 描述 |
|---------|------|
| `/bili list [目标]` | 查看订阅列表 |
| `/bili listall` (la) | 查看全部订阅 |
| `/bili listuser` (lu) `[用户]` | 查看已订阅用户 |

| 配置 | 描述 |
|------|------|
| `/bili config [用户] [目标]` | 交互式配置 (可配置主题色、模板、过滤器) |
| `/bili color <用户> <HEX颜色>` | 设置主题色 |
| `/bili templatelist` (tl) `[类型:d\|l\|le]` | 查看推送模板效果 |
| `/bili template` (t) `<类型:d\|l\|le> <模板名> [目标]` | 设置推送模板 |
| `/bili atall` (aa) `[类型] [用户] [目标]` | 添加@全体（配置后直接生效） |
| `/bili delatall` (daa) `[类型] [用户] [目标]` | 取消@全体 |
| `/bili listatall` (laa) `[用户] [目标]` | 查看@全体列表 |

| 搜索 | 描述 |
|------|------|
| `/bili search` (s) `<动态ID>` | 获取动态详情 |
| `/bili new <用户> [数量]` | 获取最新动态 |
| `/bili video <用户>` | 获取最新视频 |
| `/bili live` | 查看当前直播状态 |

| [分组指令](#分组指令) |
|-----------------|

| [动态过滤指令](#动态过滤指令) |
|---------------------|

**指令中的 `目标` 可以是 好友 / 群 / 分组**

`<..>` 尖括号为必填参数   `[..]` 中括号为可选参数
`[目标]` 不填的话默认对话所在地
`<HEX颜色>` 必须带#号 支持多个值自定义渐变 中间用分号';'分隔 例: `#fde8ed;#fde8ed`
`uid / 用户名` 使用名称可以`本地`模糊匹配（只有订阅过才能匹配）

```
# 示例
/bili add 487550002             # 为当前群/好友添加 uid 为 487550002 的订阅
/bili add 487550002 111111      # 为 111111 群/好友添加订阅
/bili color 487550002 #fde8ed   # 设置主题色
/bili t d ForwardMsg            # 为当前群/好友动态推送模板设置为ForwardMsg
```

### 动态过滤指令

推荐使用 `/bili config` 交互式配置

两种过滤器:

- 类型过滤器: 通过动态类型进行过滤 可选类型 `动态` `转发动态` `视频` `音乐` `专栏` `直播`
- 内容正则过滤器: 对动态进行正则匹配过滤
  可同时添加两种过滤器，但两种过滤器是独立先后执行的

过滤器的两种模式:

- `黑名单`：当动态匹配过滤器时**不**推送动态
- `白名单`：仅当动态匹配过滤器时**推送**动态

| 指令 | 描述 |
|------|------|
| `/bili filtertype` (ft) `<类型> [uid] [目标]` | 添加类型过滤 |
| `/bili filterreg` (fr) `<正则> [uid] [目标]` | 添加正则过滤 |
| `/bili filtermode` (fm) `<t\|r> <w\|b> [uid] [目标]` | 设置过滤模式 (t=类型/r=正则, w=白名单/b=黑名单) |
| `/bili filterlist` (fl) `[uid] [目标]` | 查看过滤列表 |
| `/bili filterdel` (fd) `<序号> [uid] [目标]` | 删除过滤项 |

```
# 示例
/bili ft 转发动态 487550002    # 为当前群/好友订阅的 487550002 设置类型为"转发动态"的过滤
/bili fr ^新年好 0 111111    # 为 111111 群/好友订阅的所有用户设置内容为"^新年好"的正则过滤
/bili fl 487550002          # 查询当前群/好友订阅的 487550002 设置过的过滤列表
/bili fd t1 487550002       # 删除索引为 t1 的过滤

# uid 为 0 时，代表群/好友订阅的所有用户（不填默认也为 0）
```

### 分组指令

| 指令 | 描述 |
|------|------|
| `/bili create <分组名>` | 创建分组 (分组名不能全为数字) |
| `/bili listgroup` (lg) `[分组名]` | 查看分组列表 |
| `/bili delgroup` (dg) `<分组名>` | 删除分组 (同时删除分组所有数据) |
| `/bili push <分组名> <联系人列表>` | 向分组添加联系人 (逗号分隔) |
| `/bili ban <分组名> <联系人列表>` | 从分组移除联系人 (逗号分隔) |
| `/bili addgroupadmin` (aga) `<分组名> <QQ号列表>` | 添加分组管理员 (逗号分隔) |
| `/bili bangroupadmin` (bga) `<分组名> <QQ号列表>` | 删除分组管理员 (逗号分隔) |

### 权限

- **管理员**：在 `OneBotConfig.yml` 中配置 `adminIds` 列表，只有管理员才能使用所有命令
- 管理员默认拥有最高权限，可管理所有群和好友的配置
- @全体功能通过 `/bili atall` 命令配置后直接生效，无需额外权限配置

## 配置

配置文件位于配置目录（默认 `./config/`）下

| 文件 | 说明 |
|------|------|
| `OneBotConfig.yml` | OneBot 连接和管理员配置 |
| `BiliConfig.yml` | 基础功能配置 |
| `ImageQuality.yml` | 图片分辨率配置 |
| `ImageTheme.yml` | 图片主题配置 |

### BiliConfig.yml

| 配置项 | 取值 | 说明 |
|--------|------|------|
| `enableConfig` | [EnableConfig](#EnableConfig) | 功能开关 |
| `accountConfig` | [BiliAccountConfig](#BiliAccountConfig) | 账号配置 |
| `checkConfig` | [CheckConfig](#CheckConfig) | 检测配置 |
| `pushConfig` | [PushConfig](#PushConfig) | 推送配置 |
| `imageConfig` | [ImageConfig](#ImageConfig) | 绘图配置 |
| `templateConfig` | [TemplateConfig](#TemplateConfig) | 模板配置 |
| `cacheConfig` | [CacheConfig](#CacheConfig) | 缓存配置 |
| `proxyConfig` | [ProxyConfig](#ProxyConfig) | 代理配置 |
| `translateConfig` | [TranslateConfig](#TranslateConfig) | 翻译配置 |
| `linkResolveConfig` | [LinkResolveConfig](#LinkResolveConfig) | 链接解析配置 |

---

#### EnableConfig

| 功能开关 | 取值 | 说明 |
|---------|------|------|
| `drawEnable` | `true` / `false` | 绘图开关 |
| `notifyEnable` | `true` / `false` | 操作通知开关 |
| `liveCloseNotifyEnable` | `true` / `false` | 直播结束通知开关 |
| `lowSpeedEnable` | `true` / `false` | 低频检测开关 |
| `translateEnable` | `true` / `false` | 翻译开关 |
| `proxyEnable` | `true` / `false` | 代理开关 |
| `cacheClearEnable` | `true` / `false` | 缓存清理开关 |

#### BiliAccountConfig

| 账号配置 | 取值 | 说明 |
|---------|------|------|
| `cookie` | SESSDATA=xxxx; bili_jct=xxxx; | B站Cookie |
| `autoFollow` | `true` / `false` | 自动关注 |
| `followGroup` | 最长16字符 | 关注时保存的分组 |

#### CheckConfig

| 检测配置 | 取值 | 说明 |
|---------|------|------|
| `interval` | 推荐 15-60 单位秒 | 动态检测间隔 |
| `liveInterval` | 单位秒 | 直播检测间隔 |
| `lowSpeed` | 例: 3-8x2 | 低频检测时间段与倍率 |
| `checkReportInterval` | 单位分 | 检测报告间隔 |
| `timeout` | 单位秒 | 超时时间 |

#### PushConfig

| 推送配置 | 取值 | 说明 |
|---------|------|------|
| `messageInterval` | 单位毫秒 | 同一个群中连续发送多个消息的间隔 |
| `pushInterval` | 单位毫秒 | 连续发送多个群之间的间隔 |
| `atAllPlus` | `SINGLE_MESSAGE` / `PLUS_END` | At全体拼接方式 |
| `toShortLink` | `true` / `false` | 是否转为短链 (不推荐) |

#### ImageConfig

| 绘图配置 | 取值 | 说明 |
|---------|------|------|
| `quality` | `800w` / `1000w` / `1200w` / `1500w` | 图片质量(分辨率) |
| `theme` | `v3` / `v3RainbowOutline` / `v2` | 绘图主题 |
| `font` | 字体名 / 字体文件名(不用后缀) | 绘图字体，放到数据目录下 `font` 文件夹中 |
| `defaultColor` | HEX颜色值 | 默认主题色 |
| `cardOrnament` | `FanCard` / `QrCode` / `None` | 卡片装饰 |
| `colorGenerator` | [ColorGenerator](#ColorGenerator) | 渐变色生成器配置 |
| `badgeEnable` | `true` / `false` | 卡片顶部标签 |

##### ColorGenerator

| 渐变色生成器 | 取值 | 说明 |
|------------|------|------|
| `hueStep` | `0` ~ `120` | 色相步长 |
| `lockSB` | `true` / `false` | 锁定饱和度和亮度 |
| `saturation` | `0.0` ~ `1.0` | 饱和度 |
| `brightness` | `0.0` ~ `1.0` | 亮度 |

#### TemplateConfig

| 模板配置 | 取值 | 说明 |
|---------|------|------|
| `defaultDynamicPush` | 模板名 | 默认动态推送模板 |
| `defaultLivePush` | 模板名 | 默认直播推送模板 |
| `defaultLiveClose` | 模板名 | 默认直播结束推送模板 |
| `dynamicPush` | Map | 动态推送模板 (可自行添加) |
| `livePush` | Map | 直播推送模板 (可自行添加) |
| `liveClose` | Map | 直播结束推送模板 (可自行添加) |
| `forwardCard` | [ForwardDisplay](#ForwardDisplay) | 转发卡片外观 |
| `footer` | [FooterConfig](#FooterConfig) | 图片页脚模板 |

##### ForwardDisplay

| 转发样式 | 说明 |
|---------|------|
| `title` | 转发卡片标题 |
| `preview` | 中间的预览 (最多4行，用`\n`隔开) |
| `summary` | 转发卡片最下边的总结 |
| `brief` | 从群外看显示的文字 |

##### 动态模板配置项

`{draw}`: 绘制的动态图
`{name}`: 名称
`{uid}`: 用户ID
`{did}`: 动态ID
`{type}`: 动态类型
`{time}`: 时间
`{content}`: 动态内容
`{images}`: 动态中的图
`{link}`: 动态链接
`{links}`: 视频专栏等有多个链接
`\n`: 换行
`\r`: 分割对话(会生成多个QQ消息)
`{>>} {<<}`: 包装成转发消息

##### 直播模板配置项

`{draw}`: 绘制的直播图
`{name}`: 名称
`{uid}`: 用户ID
`{rid}`: 房间号
`{time}`: 直播开始时间
`{title}`: 直播标题
`{area}`: 直播分区
`{cover}`: 直播封面
`{link}`: 直播链接
`\n`: 换行
`\r`: 分割对话(会生成多个QQ消息)

注: 直播模板不支持 (`{>>}{<<}`) 转发消息

##### 直播结束模板配置项

`{name}`: 名称
`{uid}`: 用户ID
`{rid}`: 房间号
`{title}`: 直播标题
`{area}`: 直播分区
`{startTime}`: 直播开始时间
`{endTime}`: 直播结束时间
`{duration}`: 直播时长
`{link}`: 直播链接
`\n`: 换行

注: 直播结束模板不支持 (`{>>}{<<}`) 转发消息 和 (`\r`) 分隔对话

##### 转发卡片配置项

`{name}`: 名称
`{uid}`: 用户ID
`{did}`: 动态ID
`{type}`: 动态类型
`{time}`: 时间
`{content}`: 动态内容
`{link}`: 链接

##### 页脚配置项

`{name}`: 名称
`{uid}`: 用户ID
`{id}`: 动态/直播ID
`{type}`: 类型
`{time}`: 时间

#### CacheConfig

| 缓存配置 | 取值 | 说明 |
|---------|------|------|
| `downloadOriginal` | `true` / `false` | 是否下载原图 |
| `expires` | 单位`天`，为 `0` 时不清理 | 图片过期时长 (DRAW/IMAGES/EMOJI/USER/OTHER) |

#### ProxyConfig

| 代理配置 | 取值 | 说明 |
|---------|------|------|
| `proxy` | 代理服务器列表 | 代理列表 |

#### TranslateConfig

| 翻译 | 取值 | 说明 |
|------|------|------|
| `cutLine` | 例:`\n\n〓〓〓 翻译 〓〓〓\n` | 正文与翻译的分割线 |
| `baidu` | `APP_ID` `SECURITY_KEY` | 百度翻译密钥 https://api.fanyi.baidu.com |

#### LinkResolveConfig

| 链接解析 | 取值 | 说明 |
|---------|------|------|
| `triggerMode` | `At` / `Always` / `Never` | 触发模式 |
| `returnLink` | `true` / `false` | 是否返回解析的链接 |
| `regex` | 正则表达式列表 | B站链接正则，支持自定义 |

## 使用帮助

### 基本原理

通过检测 [动态](https://t.bilibili.com/) 界面，检测账号关注的所有最新动态，再挑选出 QQ 订阅的动态，这样一个检测周期就可以检测所有最新动态。
因此，本应用需要一个 B 站账号来订阅用户。
**强烈推荐使用小号**

### 关于自动关注

如果账号没有关注过此人，bot 会自动关注并把他分到一个新分组中，方便管理
是否开启自动关注以及新分组的名称都可以在配置文件中进行配置

### 字体

#### [HarmonyOS Sans](https://developer.harmonyos.com/cn/docs/design/des-resources/general-0000001157315901)

选择下载字体压缩包文件，请使用压缩包内 `HarmonyOS_Sans_SC` 目录下的字体（简体中文）
不同文件代表不同粗细，建议使用 `Regular` 或 `Medium`

**下载到字体后请将字体文件放到 `数据目录/font` 文件夹内**

如果不配置字体且 `font` 目录下为空，首次启动时会自动下载 HarmonyOS Sans 字体

### 手动获取 Cookie

推荐使用 `/bili login` 指令进行登录

#### 通过开发者工具获取

建议开启浏览器无痕模式
浏览器打开 [BiliBili](https://www.bilibili.com/) 并登录
注：**登录后不要点退出登录**

按 `F12` 打开开发者工具，找到 `Network / 网络`
按 `F5` 刷新页面，复制 Cookie
将 Cookie 粘贴到配置文件 `accountConfig.cookie` 中，**使用双引号包裹**

### 图片缓存

所有图片缓存在 `数据目录/cache` 下

## 更新日志

[Releases](https://github.com//PokersKun/bilibili-dynamic-onebot/releases)

## 相关链接

[B站: 狂捡垃圾袋](https://space.bilibili.com/2467469)
[B站: Colter_null](https://space.bilibili.com/32868931)

## 感谢

Colter23: [bilibili-dynamic-mirai-plugin](https://github.com/Colter23/bilibili-dynamic-mirai-plugin)
cssxsh: [bilibili-helper](https://github.com/cssxsh/bilibili-helper)
Twitter Emoji: [Twemoji](https://github.com/twitter/twemoji)
