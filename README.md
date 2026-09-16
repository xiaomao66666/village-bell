# 村庄铃铛 · Android 0.1.0

导入《部落冲突》国服导出的村庄 JSON，自动生成升级列表和到点提醒。Android 8.0 及以上可用。应用完全离线，没有网络权限，不需要游戏账号密码。

[下载预览版 APK](https://github.com/xiaomao66666/village-bell/releases) · [更新记录](CHANGELOG.md) · [数据与隐私](docs/PRIVACY.md)

## 安装与使用

1. 从 Releases 下载 APK（或自行构建 `dist/village-bell-0.1.0.apk`），传到手机打开安装。首次安装时，系统可能要求允许该文件来源安装应用。
2. 在游戏中进入「设置 → 更多设置 → 导出村庄数据」。
3. 打开「村庄铃铛」，点击「导入村庄数据」，粘贴并确认预览；也可选择 UTF-8 编码的 `.json` / `.txt` 文件，或从其他应用分享文字/文件。不直接读取 Word。
4. 在「提醒」页允许通知和「闹钟和提醒」。准时权限未开启时会使用普通系统提醒，可能延迟。
5. 点击「发送 10 秒测试提醒」，切到桌面检查通知。部分手机需要额外允许自启动或调整电池管理，请以真机测试为准。
6. 每次安排新升级、使用药水/助手或立即完成后，重新导出并同步。APP 不会自动连接游戏服务器。

第一版只管理一个村庄。重新导入会替换任务、取消旧计时、重置单项提醒开关。较旧的同村庄数据不能覆盖新数据；重复导入同一导出时间的数据不会重建提醒。历史快照中已经到期的条目只显示「预计已完成」，不会一次性弹出历史提醒。

## 已实现

- 原生中文安卓界面，升级/提醒/使用说明三个页面，主世界与夜世界分类。
- 从文本、文件和系统分享读取 JSON，先预览再同步。
- 以**导出时间**计算完成时刻，延迟导入不会重新开始倒计时。
- 保留同类型的多座建筑，并解析 `types/modules` 中的嵌套升级。
- 未知项目显示 ID 并保留计时，避免丢失国服新项目。
- 本机持久化、单项提醒开关、总开关、数据清除。
- 系统 AlarmManager 安排下一次到期，批量通知同时到期项目；无需常驻后台服务。
- 系统重启、应用更新、时间变化以及精确闹钟权限开启后重新安排提醒。
- 通知权限提示、精确闹钟权限提示、10 秒测试通知。

## 当前边界

- 只通过用户主动导出同步；无法感知导出后的游戏变化。
- 国服助手 ID `124000001` 的具体映射尚未确认，不套用国际服助手规则。
- 若出现有效加速字段、`helper_timer > 0` 或自动循环助手 `helper_recurrent`，本版本展示基础预计时间并**暂停该快照的全部升级提醒**，页面会说明原因。加速结束后重新导出可恢复。仅有 `*_cooldown` 不会暂停提醒。
- 未知 ID 的中文名称后续补充；普通等级字段按「导出等级」展示，不盲目将所有条目解释为 `等级 + 1`。
- 应用被系统强行停止后需重新打开。省电策略、通知频道静音等仍可能影响送达。
- 当前 APK 使用本机生成的开发签名，适合自用试装，不是应用商店发布版本。后续覆盖升级应保留同一签名。

## 验证记录

- 2026-09-16：成功编译 SDK 35 / minSdk 26 APK；APK v2、v3 签名验证通过。
- `test-parser.ps1`：26 项 JVM 检查。人工合成样例包含 12 项升级和 4 项夜世界升级；另覆盖最早/最晚项目及时间、重复建筑、嵌套模块、未知 ID、延迟导入、冷却与加速区别、循环助手、截断/部分数据、异常计时、未来时间、BOM、旧快照等。开发时也用本机国服完整导出验证过解析，真实数据不在仓库中。
- APK manifest 检查：无网络权限，启动 Activity 与三个提醒相关权限符合预期。
- 当前没有连接的安卓设备；**尚未完成安装、界面、锁屏通知、重启恢复和厂商省电策略的实机验证**。编译及解析测试不能替代这些测试。

建议真机验收：首次空状态 → 导入完整数据 → 查看 12 项 → 10 秒测试 → 关闭/恢复提醒 → 导入更新数据 → 检查旧提醒取消 → 重启恢复。存在加速时应看到提醒暂停说明。旧数据和截断数据应拒绝并保留原数据。

## 构建

### Windows / PowerShell

需要 PowerShell 7 和 JDK 17 或更高版本。将 `JAVA_HOME` 指向 JDK，或给脚本传入 `-JavaHome` 参数。脚本也可以尝试从 PATH 中找到 `javac`。

```powershell
.\setup-tools.ps1
.\test-parser.ps1
.\build-apk.ps1
```

构建脚本使用 Google 官方 SDK 的 aapt2 / D8 / zipalign / apksigner，直接编译原生 Java；无需安装 Android Studio。Google SDK 包下载后核对固定 SHA-1，测试依赖固定 SHA-256。开发签名存放在 `.tools/development.keystore`，请在本机保留以便覆盖安装，不要上传。

可使用 `./test-parser.ps1 -SamplePath 'path/to/export.json'` 额外验证自己的样本。默认测试仅使用仓库中的人工合成样例，不会自动读取本机真实数据。

推送到 `main` 或提交 Pull Request 后，GitHub Actions 会在 Windows + JDK 17 上执行测试、构建并保存 14 天的 APK 构建产物。CI 使用每次运行新生成的开发签名，不能保证与 Releases 的 APK 覆盖安装兼容。

也提供 Android Studio / Gradle 项目文件（AGP 8.9.1，SDK 35，使用兼容的 Gradle 8.11.1 与 JDK 17）。此路径尚未在本机运行验证；实际交付 APK 来自上面的原生构建脚本。应用没有第三方运行时依赖；JVM 解析测试使用单独的 org.json 库。

### 文件结构

- `app/src/main/java/cn/villagebell/Village.java`：数据校验、升级提取、基础时间计算和名称映射。
- `State.java`：本机快照、开关、已通知记录。
- `Reminders.java`：系统通知、下次闹钟、批量到期处理。
- `MainActivity.java`：导入预览、升级列表和权限设置。
- `tests/ParserTest.java`：独立 JVM 解析回归检查。
- `tests/fixtures/`：人工合成的可公开测试样例。
- `local-data/`：可选的本机真实样本存放位置，已排除版本控制，不打包进 APK。
- `.github/workflows/build.yml`：自动测试和 APK 构建。
- `docs/`：隐私说明和发布说明。

## 参考与实现来源

本项目自行实现解析、界面和调度，没有直接复制以下项目的源码：

- [COC_Timer](https://github.com/NightOwlEyes/COC_Timer)：参考数据结构和功能范围，原项目 GPL-3.0。
- [coc-tracker](https://github.com/rahulkhatri137/coc-tracker)：参考公开数据 ID 与名称对应关系，以及安卓提醒功能范围。
- [Android 系统闹钟文档](https://developer.android.com/develop/background-work/services/alarms)。
- [Android 通知权限文档](https://developer.android.com/develop/ui/compose/notifications/notification-permission)。

这是非官方辅助工具，不隶属于 Supercell。
