#!/bin/bash

set -x

TAG=$(git describe --abbrev=0)
PREV_TAG=$(git describe --abbrev=0 HEAD^)
PLUGIN_VERSION=$(cat build.gradle |grep pluginVersionName|cut -d \" -f 2)

if [ ! -z "$TRAVIS_BUILD_ID" ]; then
    MD5_LINE=$(curl -s https://api.travis-ci.org/v3/job/$TRAVIS_JOB_ID/log.txt | grep -n 'exec md5sum' | cut -d : -f 1 | head -n 1)
fi

echo "
[![Download (github release)](https://img.shields.io/github/downloads/$TRAVIS_REPO_SLUG/$TAG/total.svg)](https://github.com/$TRAVIS_REPO_SLUG/releases/download/$TAG/callerinfo-$TAG-release.apk)
[![Download (github release)](https://img.shields.io/github/downloads/$TRAVIS_REPO_SLUG/$TAG/CallerInfo-plugin-v$PLUGIN_VERSION-full-release.apk.svg)](https://github.com/$TRAVIS_REPO_SLUG/releases/download/$TAG/CallerInfo-plugin-v$PLUGIN_VERSION-full-release.apk)

---

Release files are generated and deployed by Travis-ci, check sha1 and md5 from **build log:**

[https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID#L$MD5_LINE](https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID#L$MD5_LINE)
"

echo "## commits"
git --no-pager log $PREV_TAG...$TAG \
    --pretty=format:" - [%s](https://github.com/$TRAVIS_REPO_SLUG/commit/%H)" \
    --reverse | \
    grep -v 'Merge pull request' | \
    grep -v 'build' | \
    grep -v 'bump'

echo "
## changes

- [$PREV_TAG - $TAG](https://github.com/$TRAVIS_REPO_SLUG/compare/$PREV_TAG...$TAG?diff=unified)
"
