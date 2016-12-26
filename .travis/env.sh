#!/bin/bash
 
# This script generates environment variables for pull requests and forks.
 
if [ -z "$encrypted_75846693d905_key" ] ; then
    # It's running from pull requests or forks, set vars.
     
    TEXT="I_AM_PUBLIC_AND_NOT_USED_FOR_RELEASE"
     
    # encrypted key and iv is taking from 'openssl enc -nosalt -aes-256-cbc -pass pass:I_AM_PUBLIC_AND_NOT_USED_FOR_RELEASE -P'
     
    export encrypted_75846693d905_key="12CF1B5E0D192628AA922230549EEDFD889E6CF7463933C6DABD9A1300FCA23D"
    export encrypted_75846693d905_iv="66813CF28D04CD129D57436B78DECBA4"
     
    export GITHUB_TOKEN="$TEXT"
    export KEYSTORE_PASSWORD="$TEXT"
    export ALIAS_PASSWORD="$TEXT"
    export ALIAS="$TEXT"
    export API_KEY="$TEXT"
    export JUHE_API_KEY="$TEXT"
    export LEANCLOUD_APP_ID="$TEXT"
    export LEANCLOUD_APP_KEY="$TEXT"
     
    # Overlay release.jks.enc
     
    # Travis-ci is using 'openssl aes-256-cbc -K 12CF1B5E0D192628AA922230549EEDFD889E6CF7463933C6DABD9A1300FCA23D -iv 66813CF28D04CD129D57436B78DECBA4 -in public.jks.enc -out public.jks -d' to decrypt the file.
    mv "public.jks.enc" "release.jks.enc"
fi
     
