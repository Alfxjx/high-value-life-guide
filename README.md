# 高性价比人生指南（Android 离线阅读 App）

一个完全离线的 Android 阅读器：把开源书籍《高性价比人生指南》的单文件 HTML（自带全文搜索与明/暗主题切换，零外部资源）打包进 APK，用全屏 WebView 呈现。安装后无需网络即可阅读全书。

## 功能特性

- **完全离线**：书籍内容打包在 APK 内，不申请任何权限（包括 INTERNET），无广告、无追踪
- **全文搜索**：内容页自带搜索功能
- **明/暗主题**：跟随内容页内置的主题切换
- **Edge-to-edge**：适配 Android 15（targetSdk 35）全屏显示规范
- **外链处理**：书内 http/https 链接自动跳转系统浏览器打开

## 下载 APK

在 [GitHub Releases](../../releases) 页面下载最新版 `app-release.apk`，直接安装即可。

## 本地构建

前置条件：Android SDK（platforms android-35、build-tools 34.0.0），Java 17。

macOS 上若系统 Java 版本低于 17，可直接使用 Android Studio 内嵌的 JBR——本仓库的 `gradle.properties` 已默认指向：

```
org.gradle.java.home=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

如你的 Android Studio 安装路径不同，或本机已有 Java 17+，请修改/删除该行（建议改到未入库的 `local.properties` 中）。

构建步骤：

```bash
# 1. 拉取最新书籍内容（curl 下载并校验 > 1MB）
./scripts/fetch-content.sh

# 2. 构建
./gradlew assembleDebug      # 调试包
./gradlew assembleRelease    # 发布包（需签名配置，见下）
```

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
2. 内容有变化时由 bot 提交回仓库
3. 构建签名 release APK（`versionCode` = workflow run 号，`versionName` = 当天日期）
4. 以 `v<日期>` 为 tag 发布 GitHub Release 并附上 APK

手动触发：Actions 页面运行该 workflow 即可（workflow_dispatch 会强制走完整发布流程）。

### 仓库需要配置的 Secrets

| Secret | 内容 |
| --- | --- |
| `KEYSTORE_BASE64` | keystore 文件的 base64（`base64 -i hvlg-upload.keystore \| pbcopy`） |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | 密钥别名（如 `hvlg`） |
| `KEY_PASSWORD` | 密钥密码 |

## 内容与授权

- 书籍内容来自开源项目，上游仓库：[eternity4719/HowToLiveBetter](https://github.com/eternity4719/HowToLiveBetter)、[cdyforever/how-to-live-better](https://github.com/cdyforever/how-to-live-better)
- 原书以 [Unlicense](https://unlicense.org/) 发布，属于公有领域，可自由使用、修改、分发
- 本仓库代码以 MIT 协议发布
