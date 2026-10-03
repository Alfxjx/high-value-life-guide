# 高性价比人生指南（Android 离线阅读器）

一个完全离线的 Android 阅读器：把开源书籍《高性价比人生指南》的单文件 HTML（34 节、650 条建议）在**构建期**解析成结构化 JSON，再用 Jetpack Compose + Material 3 全原生重画。安装后无需网络即可阅读全书。

## 功能特性

- **完全离线**：内容以 `content.json` 打包在 APK 内，不申请 INTERNET 等任何权限，无广告、无追踪
- **上次看到哪里**：滚动停止后记住当前卡片，冷启动自动跳回并提示「已回到上次位置 · 第 N 节」，可一键回到开头
- **侧边抽屉目录**：34 节分组，可展开列出每条建议（序号 + 标题 + 性价比色点），点击直达；当前可见条目自动高亮
- **全文搜索**：输入即过滤，命中处底色高亮，实时显示「N / 650」计数
- **三档筛选**：全部 / 只看 A 级 / 只说人话（只保留标题、标签和说人话段落）
- **明暗主题**：默认跟随系统，可手动切换并记忆；状态栏与导航栏图标随主题联动
- **文献来源**：每张卡片可展开查看原始文献，带链接的可直接跳系统浏览器
- **Edge-to-edge**：适配 Android 15（targetSdk 35）全屏显示规范

## 下载 APK

在 [GitHub Releases](../../releases) 页面下载最新版 `app-release.apk`，直接安装即可。

## 本地构建

前置条件：Android SDK（platforms android-35、build-tools 34.0.0），Java 17，Python 3（解析脚本只用标准库）。

macOS 上若系统 Java 版本低于 17，可直接使用 Android Studio 内嵌的 JBR——本仓库的 `gradle.properties` 已默认指向：

```
org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

如你的 Android Studio 安装路径不同，或本机已有 Java 17+，请修改/删除该行（建议改到未入库的 `local.properties` 中）。

构建步骤：

```bash
# 1. 拉取最新书籍内容（curl 下载并校验 > 1MB）
./scripts/fetch-content.sh

# 2. 把 index.html 解析成 assets/content.json（内置 34 节 / 650 卡 / 等级数量 /
#    性价比数量 / 1537 条外链等断言，任何一条不满足就非零退出，不会把坏数据打进 APK）
python3 scripts/parse_content.py

# 3. 构建
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 发布包（需签名配置，见下）
```

第 2 步是必需的：`content.json` 是 Compose 界面唯一的数据来源，缺了它 App 起不来。

### 签名配置（release）

在项目根目录创建 `local.properties`（已 gitignore）：

```properties
KEYSTORE_FILE=/绝对路径/到你的.keystore
KEYSTORE_PASSWORD=你的keystore密码
KEY_ALIAS=你的别名
KEY_PASSWORD=你的密钥密码
```

没有 keystore 时可用 keytool 生成：

```bash
keytool -genkeypair -keystore hvlg-upload.keystore -alias hvlg \
  -keyalg RSA -keysize 4096 -validity 10950
```

`local.properties` 不存在时会跳过签名配置（CI 使用此机制），此时 release 构建产物为未签名 APK。

## 内容自动更新

GitHub Actions 每天北京时间 06:30 自动运行 `.github/workflows/update-and-release.yml`：

1. 执行 `scripts/fetch-content.sh` 拉取上游最新 `index.html`
2. 执行 `scripts/parse_content.py` 重新生成 `content.json`
3. `index.html` 或 `content.json` 任一有变化时由 bot 提交回仓库
4. 构建签名 release APK（`versionCode` = workflow run 号，`versionName` = 当天日期）
5. 以 `v<日期>` 为 tag 发布 GitHub Release 并附上 APK

手动触发：Actions 页面运行该 workflow 即可（workflow_dispatch 会强制走完整发布流程）。

### 仓库需要配置的 Secrets

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore 文件的 base64（`base64 -i hvlg-upload.keystore \| pbcopy`） |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 密钥别名（如 `hvlg`） |
| `KEY_PASSWORD` | 密钥密码 |

## 代码结构

```
scripts/parse_content.py                    # index.html -> content.json（构建期解析 + 断言）
app/src/main/assets/index.html              # 上游原始单文件书页（解析输入，仍随仓库更新）
app/src/main/assets/content.json            # 解析产物（构建期生成，入库以便 CI 比对变化）
app/src/main/java/com/hvlg/guide/
  MainActivity.kt                           # 唯一 Activity，enableEdgeToEdge + setContent
  data/Models.kt                            # @Serializable 数据模型
  data/ContentRepository.kt                 # assets 读取 + 内存索引
  data/SettingsRepository.kt                # DataStore：lastCardId / themeMode
  ui/ReaderViewModel.kt                     # 状态与业务逻辑
  ui/ReaderScreen.kt                        # Scaffold / TopBar / 搜索 / 筛选 / 抽屉 / FAB
  ui/CardItem.kt                            # 卡片（标签、说人话、字段、来源）
  ui/Toc.kt                                 # 抽屉目录
  ui/Theme.kt                               # 明暗色板与系统栏联动
  ui/Icons.kt                               # 手绘太阳/月亮图标
  ui/Components.kt                          # 徽章、色点、序号
  ui/Highlight.kt                           # 搜索命中高亮
```

## 内容与授权

- 书籍内容来自开源项目，上游仓库：[eternity4719/HowToLiveBetter](https://github.com/eternity4719/HowToLiveBetter)、[cdyforever/how-to-live-better](https://github.com/cdyforever/how-to-live-better)
- 原书以 [Unlicense](https://unlicense.org/) 发布，属于公有领域，可自由使用、修改、分发
- 本仓库代码以 MIT 协议发布
