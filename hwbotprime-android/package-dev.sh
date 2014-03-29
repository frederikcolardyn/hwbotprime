jarsigner -verbose -sigalg SHA1withRSA -digestalg SHA1 -keystore /Users/frederik/dev/git/hwbotprime-security/hwbotprime-security/hwbot.keystore ~/Desktop/hwbotprime-android-dev.apk hwbot
jarsigner -verify ~/Desktop/hwbotprime-android-dev.apk
mv ~/Desktop/hwbotprime-android-dev.apk ~/Desktop/hwbotprime-android-unaligned.apk
/Users/frederik/android-sdks/tools/zipalign -v 4  ~/Desktop/hwbotprime-android-unaligned.apk ~/Desktop/hwbotprime-android-dev.apk
s3cmd put ~/Desktop/hwbotprime-android-dev.apk s3://hwbotdownloads 
echo done! Do not forget to make s3 file public.
