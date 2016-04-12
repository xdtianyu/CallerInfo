# 来电信息
一个获取来电归属地和其他信息(如推销、诈骗)的安卓应用。[Beta 测试及交流社群](https://plus.google.com/communities/109266984412695150545) [English](https://github.com/xdtianyu/CallerInfo/blob/master/README-EN.md)

[![Google Play](https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/en-play-badge.png)](https://play.google.com/store/apps/details?id=org.xdty.callerinfo)

[![Build Status](https://travis-ci.org/xdtianyu/CallerInfo.svg?branch=master)](https://travis-ci.org/xdtianyu/CallerInfo)
[![Release notes](https://img.shields.io/badge/release-notes-yellowgreen.svg)](https://github.com/xdtianyu/CallerInfo/releases)
[![Coolapk download](https://img.shields.io/badge/coolapk-download-blue.svg)](http://coolapk.com/apk/org.xdty.callerinfo)

[![下载地址 (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/v1.4.0/total.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/v1.4.0/callerinfo-v1.4.0-release.apk)

[![下载地址 (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/v1.3.3/CallerInfo-plugin-v1.0.1-release.apk.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/v1.3.3/CallerInfo-plugin-v1.0.1-release.apk)

## 功能

1\. 查询来电号码并显示悬浮窗，显示位置和其他号码信息。
 
2\. 主界面显示最近来电列表(不会导入已有的系统通讯录)。
 
3\. 在主界面可以查询任意电话号码信息。
 
4\. 自定义设置卡片及悬浮窗的颜色。
 
5\. 自定义悬浮窗, 如文字大小、透明度、位置。可以自定义显示与隐藏，如忽略已存在的联系人、去电时显示、接听后隐藏。
 
6\. 离线查询。优先从本地的离线归属地及历史记录中查询数据并显示，没有查询到标记数据且有网络(有 WIFI 或手机未掉网)时会联网查询。
 
**隐藏功能 (点击七次版本后出现)**
 
1\. 自定义数据源。可以自定义百度、聚合数据(360)的 API 密钥，可以自定义 API (用于适配客户信息系统)。可以设置忽略号码段来忽略查询，可以强制使用本地离线数据。
 
**插件功能(安装插件后出现)**
 
1\. 自动挂断。可以自动挂断匹配的标记关键字(诈骗、广告等)、归属地、起始号码(400*)。归属地可逆向匹配，如 "!西安 !咸阳" 将挂断所有除 "西安" "咸阳" 的来电。起始号码关键字添加完整号码并以空格分隔可以实现 "黑名单" 的功能。
 
2\. 添加号码信息到系统通话记录。会添加诈骗、骚扰、广告、响一声、自动挂断等信息到系统通话记录。

## 说明
 
1\. “来电信息” 的不断改进和完善离不开社区的反馈，非常感谢所有在 Play 市场、V站、酷市场留言及发送邮件反馈的朋友。
 
2\. 应用开源免费无广告，请放心使用。APK 文件通过 Travis CI 自动生成并上传，用户可在每个 [GitHub 版本发行](https://github.com/xdtianyu/CallerInfo/releases) 中找到编译日志来校验文件 sha1 及 md5。
 
3\. 请避免限制主应用及插件请求的权限。如出现来电时不显示悬浮窗，请先检查权限设置、权限管理类型应用的设置。如果安装了插件，请确保插件和主应用没有进入管理类软件的黑名单或优化项目。
 
4\. 如果对此开源应用有任何不满、问题或建议，请在 [GitHub](https://github.com/xdtianyu/CallerInfo/issues) 提交问题单或发送问题到作者邮件。非常欢迎大家反馈，来和作者一起完善这个应用。
 
5\. 如果您觉得这个应用做的不错，欢迎在 [GitHub](https://github.com/xdtianyu/CallerInfo) star、在 [Play 市场](https://play.google.com/store/apps/details?id=org.xdty.callerinfo) 及 [酷市场](http://coolapk.com/apk/org.xdty.callerinfo) 五星好评，欢迎您将此应用通过推特、微博、朋友圈等社交网络推广给更多的人。

## 贡献者
[tianyu](https://www.xdty.org) 

[blueset](https://github.com/blueset) 修复英语翻译

## 屏幕截图
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/1.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/2.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/3.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/4.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/5.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/6.png" alt="screenshot" width="300">

<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/p-1.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/p-2.png" alt="screenshot" width="300">

## 感谢

[PhoneNumber](https://github.com/xdtianyu/PhoneNumber): A library to get phone number location and other info from baidu api.

[ColorPicker](https://github.com/xdtianyu/ColorPicker): An easy to use android color picker library. Based on android-colorpicker.

[Sugar ORM](https://github.com/satyan/sugar): Insanely easy way to work with Android databases.

[Android-MaterialPreference](https://github.com/jenzz/Android-MaterialPreference): A simple backward-compatible implementation of a Material Design Preference aka settings item

[StandOut](https://github.com/pingpongboss/StandOut): A library to let you easily create floating windows in your Android app.

[SeekBarCompat](https://github.com/ahmedrizwan/SeekBarCompat): A simple material-based support library to bring consistent SeekBars on Android 14 and above.

[CustomActivityOnCrash](https://github.com/Ereza/CustomActivityOnCrash): An android library that allows launching a custom activity when your app crashes.

## [许可证](https://github.com/xdtianyu/CallerInfo/blob/master/LICENSE.md)

```
                    GNU GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.
 ```
