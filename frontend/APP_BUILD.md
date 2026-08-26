# CCPS 屋主端 App

当前 App 使用 Capacitor 8 封装现有 Vue 3 屋主端，同时生成 Android 和 iOS 原生工程。

## 重要：正式包名前置条件

`capacitor.config.json` 中的 `appId` 目前是测试标识 `com.ccps.owner`。正式更新旧 App 前，必须替换成旧 Android 包名和旧 iOS Bundle ID，并重新生成原生工程，否则无法覆盖升级旧版本。

## 测试版构建

```bash
cd frontend
npm install
npm run app:sync
npm run app:open:android
```

当前 `.env.app` 指向现有 HTTP 测试服务器，并临时开启了 cleartext。正式上架前必须完成以下调整：

1. 将 API 切换到正式 HTTPS 域名。
2. 将 `server.cleartext` 改为 `false`。
3. 使用旧 Android 签名密钥和旧 iOS Bundle ID。
4. 在真机验证登录 Cookie、文件上传、PDF 下载和外部链接。

## iOS

iOS 工程可以同步生成，但最终构建、签名和真机测试必须在安装 Xcode 的 macOS 上进行：

```bash
npm run app:open:ios
```

## 日常同步

修改 Vue 页面后执行：

```bash
npm run app:sync
```
