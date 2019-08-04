#!/bin/bash

TAG=$(git describe --abbrev=0)
PREV_TAG=$(git describe --abbrev=0 HEAD^)
PLUGIN_VERSION=$(cat build.gradle |grep pluginVersionName|cut -d \" -f 2)

MD5_LINE=$(curl -s https://api.travis-ci.org/v3/job/$TRAVIS_BUILD_ID/log.txt | grep -n 'exec md5sum' | cut -d : -f 1 | head -n 1)

echo "
[![Download (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/$TAG/total.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/$TAG/callerinfo-$TAG-release.apk)
[![Download (github release)](https://img.shields.io/github/downloads/xdtianyu/CallerInfo/$TAG/CallerInfo-plugin-v$PLUGIN_VERSION-full-release.apk.svg)](https://github.com/xdtianyu/CallerInfo/releases/download/$TAG/CallerInfo-plugin-v$PLUGIN_VERSION-full-release.apk)

---

Release files are generated and deployed by Travis-ci, check sha1 and md5 from **build log:**

[https://travis-ci.org/xdtianyu/CallerInfo/builds/$TRAVIS_BUILD_ID#L$MD5_LINE](https://travis-ci.org/xdtianyu/CallerInfo/builds/$TRAVIS_BUILD_ID#L$MD5_LINE)
"

echo "## commits"
git --no-pager log $PREV_TAG...$TAG \
    --pretty=format:' - [%s](https://github.com/xdtianyu/CallerInfo/commit/%H)' \
    --reverse | \
    grep -v 'Merge pull request' | \
    grep -v 'build' | \
    grep -v 'bump'
