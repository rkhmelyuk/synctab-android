#!/bin/sh

cd ~/projects/synctab/synctab-android/bin

STOREPASS=`cat ../key/storepass`

cp ../out/production/synctab-android/synctab-android.apk.unsigned ./synctab.apk
#jarsigner -verbose -keystore ../key/synctab.keystore -storepass "lOOnapArk72#91" synctab.apk khmlabs
jarsigner -verbose -keystore ../key/synctab.keystore -storepass "$STOREPASS" synctab.apk khmlabs
/usr/local/android/tools/zipalign -v 4 synctab.apk synctab-aligned.apk
rm synctab.apk
mv synctab-aligned.apk synctab-3.apk
