# CallerInfo
一An Android app that captures call attribution and other information (e.g. spam, scam). [Beta testing and exchange community](https://plus.google.com/communities/109266984412695150545) [English](https://github.com/xdtianyu/CallerInfo/blob/master/README-EN.md) [Developer documentation](https://github.com/xdtianyu/CallerInfo/blob/master/DEVELOPMENT-CN.md) [Telegram exchange group](https://t.me/callerinfo)

[![Google Play](https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/en-play-badge.png)](https://play.google.com/store/apps/details?id=org.xdty.callerinfo)

[![Build Status](https://travis-ci.org/xdtianyu/CallerInfo.svg?branch=master)](https://travis-ci.org/xdtianyu/CallerInfo)
[![Build status](https://ci.appveyor.com/api/projects/status/0iyva2apl5nxopxo?svg=true)](https://ci.appveyor.com/project/xdtianyu/callerinfo)
[![Build Status](https://img.shields.io/jenkins/s/https/jenkins.xdty.org/callerinfo.svg?label=jenkins)](https://jenkins.xdty.org/job/CallerInfo/buildTimeTrend)
[![pipeline status](https://git.xdty.org/tianyu/CallerInfo/badges/master/pipeline.svg)](https://git.xdty.org/tianyu/CallerInfo/-/commits/master)
[![Release notes](https://img.shields.io/badge/release-notes-yellowgreen.svg)](https://github.com/xdtianyu/CallerInfo/releases)
[![Coolapk download](https://img.shields.io/badge/coolapk-download-blue.svg)](http://coolapk.com/apk/org.xdty.callerinfo)

[![Download Link (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/v2.5.2/total.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/v2.5.2/callerinfo-v2.5.2-release.apk)

[![Download Link (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/v2.5.1/CallerInfo-plugin-v1.1.2-release.apk.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/v2.5.1/CallerInfo-plugin-v1.1.2-release.apk)

## Function

1\. Query the incoming call number and display the floating window, displaying the location and other number information.
 
2\. The main interface displays the list of recent incoming calls (the existing system address book will not be imported).
 
3\. In the main interface, you can query any phone number information.
 
4\. Customize the color of the card and floating window.
 
5\. Customize the floating window, such as text size, transparency, position. You can customize the display and hiding, such as ignoring existing contacts, displaying when an outgoing call is made, and hiding after answering.
 
6\. Offline query. Prioritize the query and display of data from the local offline attribution and historical records. When there is no marked data and there is a network (with WIFI or the mobile phone is not disconnected), it will query online.
 
**Hidden features (appears after seven clicks on the version)**
 
1\. Custom data sources. You can customize the API key of Baidu and aggregated data (360), and you can customize the API (for adapting to the customer information system). You can set the ignore number segment to ignore the query, and you can force the use of local offline data.。
 
**Plug-in function (appears after installing the plug-in)**
 
1\. Automatically hang up. It can automatically hang up matching tagged keywords (fraud, advertisement, etc.), attribution, and starting number (400*). The attribution can be matched in reverse, such as "!Xi'an !Xianyang" will hang up all incoming calls except "Xi'an" and "Xianyang". The start number keyword can be added to the complete number and separated by spaces to achieve the "blacklist" function.
 
2\. Add number information to the system call log. Information such as fraud, harassment, advertisement, beep, and automatic hangup will be added to the system call log.

## Illustrate
 
1\. The continuous improvement and improvement of "Incoming Call Information" is inseparable from the feedback of the community. I am very grateful to all the friends who leave messages and send email feedback in the Play Market, Station V, Cool Market.
 
2\. The application is open source, free of charge and has no advertisements, please feel free to use it. The APK file is automatically generated and uploaded by Travis CI, and users can find the compilation log in each [GitHub release](https://github.com/xdtianyu/CallerInfo/releases) to verify the file sha1 and md5.
 
3\. Please avoid restricting the permissions requested by the main application and plugins. If the floating window is not displayed when there is an incoming call, please check the permission setting and the setting of the permission management type application first. If a plug-in is installed, please make sure that the plug-in and the main application are not blacklisted or optimized for management software.
 
4\. If you have any dissatisfaction, questions or suggestions about this open source application, please submit a ticket on [GitHub](https://github.com/xdtianyu/CallerInfo/issues) or send the question to the author's email. Feedback is very welcome, come and improve this application together with the author.
 
5\. If you think this app is doing well, welcome to [GitHub](https://github.com/xdtianyu/CallerInfo) star, [Play Market](https://play.google.com/store/ apps/details?id=org.xdty.callerinfo) and [Cool Market](http://coolapk.com/apk/org.xdty.callerinfo) five-star praise, you are welcome to use this app on Twitter, Weibo, friends Circle and other social networks to promote to more people.

## Contributor
[tianyu](https://www.xdty.org) Main contributor

[blueset](https://github.com/blueset) fix english translation

[Soyofuki](https://github.com/Soyofuki) add Japanese translation

## Screenshots
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/1.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/2.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/3.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/4.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/5.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/6.png" alt="screenshot" width="300">

<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/p-1.png" alt="screenshot" width="300">
<img src="https://raw.githubusercontent.com/xdtianyu/CallerInfo/master/screenshots/p-2.png" alt="screenshot" width="300">

## Appreciations

[PhoneNumber](https://github.com/xdtianyu/PhoneNumber): A library to get phone number location and other info from baidu api.

[ColorPicker](https://github.com/xdtianyu/ColorPicker): An easy to use android color picker library. Based on android-colorpicker.

[Sugar ORM](https://github.com/satyan/sugar): Insanely easy way to work with Android databases.

[Android-MaterialPreference](https://github.com/jenzz/Android-MaterialPreference): A simple backward-compatible implementation of a Material Design Preference aka settings item

[StandOut](https://github.com/pingpongboss/StandOut): A library to let you easily create floating windows in your Android app.

[SeekBarCompat](https://github.com/ahmedrizwan/SeekBarCompat): A simple material-based support library to bring consistent SeekBars on Android 14 and above.

[CustomActivityOnCrash](https://github.com/Ereza/CustomActivityOnCrash): An android library that allows launching a custom activity when your app crashes.

## [License](https://github.com/xdtianyu/CallerInfo/blob/master/LICENSE.md)

```
                    GNU GENERAL PUBLIC LICENSE
                       Version 3, 29 June 2007

 Copyright (C) 2007 Free Software Foundation, Inc. <http://fsf.org/>
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.
 ```
