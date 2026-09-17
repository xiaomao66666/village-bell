# 游戏素材来源

本应用的建筑、英雄、兵种、法术和战宠图片来自 [ClashKing Assets](https://github.com/ClashKingInc/ClashKingAssets)，游戏素材版权属于 Supercell。图源仓库允许项目使用其托管素材并要求署名；仓库的 GPL-3.0 文本随本项目保存在 [LICENSE.ClashKing.txt](LICENSE.ClashKing.txt)。未复制其提取器或应用代码。

本内容非官方且未经 Supercell 认可。更多信息参见 [Supercell 粉丝内容政策](https://supercell.com/en/fan-content-policy/)。图片用于识别与说明游戏中的升级项目。应用免费，不包含游戏自动操作。

素材固定于 `d84d7fa19e546047b6d1acf6f00c14519da8c00f`，771 张原始 WebP 未重绘、改色或裁切，在界面上保持比例缩放。每张图片的原始地址和 SHA-256 记录在 [game-assets.json](game-assets.json)。完整上游源材料及对应许可可以从上述固定版本获取。

建筑按项目 ID 与导出的等级精确匹配；没有对应图片的项目使用原创通用图标，不借用其他等级。角色使用常规图标，不推断皮肤。国际服素材与国服个别外观可能存在差异。

图片封装在 `app/src/main/res/raw/game_icons.zip`，应用首次使用时复制到本机缓存并按需读取，不需要联网。可运行 `python tools/fetch-game-icons.py` 从固定版本重建，脚本需要 Python 3 与 GitHub CLI，并逐个验证 Git blob 哈希。

桌面启动图标、村庄插画和通用图标均为本项目原创绘制，不使用游戏商标作为应用图标。
