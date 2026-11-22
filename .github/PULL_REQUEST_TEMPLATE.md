## 这个 PR 做了什么

<!-- 一句话说清：修了什么 bug / 加了什么功能 -->

## 改动范围

- [ ] `app/java`（输入逻辑 / 界面）
- [ ] `app/res` 或 `app/AndroidManifest.xml`
- [ ] `app/assets`（词库、许可文本）
- [ ] `tools/`（打包或词库脚本）
- [ ] 文档（README / BUILDING / SECURITY / PRIVACY）

## 自查清单（合并前请逐条确认）

- [ ] `python tools/build_apk.py` 能打出包，`apksigner verify --print-certs` 通过
- [ ] 清单仍然是 **0 权限**、`allowBackup="false"`、`usesCleartextTraffic="false"`
- [ ] 代码里没有新增网络调用（HTTP / Socket / WebView / 远程配置拉取）
- [ ] 没有提交 `*.keystore` / `*.jks` / `*.p12` / `pass.txt` / 任何口令、token、本机绝对路径
- [ ] 没有提交 `tools/build/`、`*.apk`、日志
- [ ] 改动到词库时，`LICENSES/` 与 `app/assets/licenses/` 的来源说明同步更新
- [ ] 版本号没被乱改（要发版时改 `VERSION` 与 `--version-code` 两处）

## 说明

<!-- 有争议点、兼容性影响、需要维护者决定的，写在这里 -->
