[来电信息](https://github.com/xdtianyu/CallerInfo) 开发者文档

注意此文档仅供开发者使用，用于编译源码等。如果是初学者，对于文档中不明白的内容或遇到错误，请务必优先 Google 搜索。

----------------

**1\. 下载最新源码**

```
git clone https://github.com/xdtianyu/CallerInfo.git
cd CallerInfo/
git submodule update --init --recursive
```

**2\. 配置编译环境**

参考 `.travis/env.sh` 文件，首先解码内置的用于公开使用的 `release.jsk` 文件

```
openssl aes-256-cbc -K 12CF1B5E0D192628AA922230549EEDFD889E6CF7463933C6DABD9A1300FCA23D -iv 66813CF28D04CD129D57436B78DECBA4 -in public.jks.enc -out public.jks -d
mv public.jks release.jks
```

导出环境变量，注意修改 `ANDROID_HOME` 为你的 `Android SDK` 目录

```
export ANDROID_HOME=/home/ty/Android/Sdk

TEXT="I_AM_PUBLIC_AND_NOT_USED_FOR_RELEASE"
export KEYSTORE_PASSWORD="$TEXT"
export ALIAS_PASSWORD="$TEXT"
export ALIAS="$TEXT"
```

**3\. 运行编译**

```
./gradlew assembleDebug
```

或使用如下命令生成 `release` 版本

```
./gradlew assembleRelease
```

编译成功后，最终会在 `CallerInfo/app/build/outputs/apk` 目录生成 `CallerInfo-v2.1.5-debug.apk` 及 `CallerInfo-v2.1.5-release.apk` 文件。注意版本号可能会增加。


**4\. 环境变量**

参考 `.travis/env.sh` 文件， 上文用到的 `KEYSTORE_PASSWORD` `ALIAS_PASSWORD` `ALIAS` 都是 `public.jks` 文件相关验证，`GITHUB_TOKEN` 用于作者 `GitHub Release` 自动部署。

`API_KEY` 是百度号码服务 `API` 密钥，由于服务终止，已停止使用。`JUHE_API_KEY` 是聚合数据(360) 数据源 `API` 密钥。 

`LEANCLOUD_APP_ID` 和 `LEANCLOUD_APP_KEY` 是 `LeanCloud` 的 `API` 凭证，用于接收用户上报的号码数据。

**5\. 版本配置说明**

版本配置在 `gradle.properties` 文件中。

**6\. 部分文件说明**

`manifest.gradle` 是用于自动写入环境变量到 `manifest` 的配置文件，可以将环境变量或 `manifest.properties` 文件内的配置导入。

`signing.gradle` 类似 `manifest.gradle` 文件，用于导入环境变量或 `signing.properties` 文件内容，用于编译时证书验证配置的自动化。

**7\. Android Studio 说明**

`Android Studio` 导入项目后，需要修改上文提到的 `manifest.properties` `signing.properties` 文件来导入环境变量。


