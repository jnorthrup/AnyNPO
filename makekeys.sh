#!/bin/bash 

echo "keytool -genkeypair -alias $1 -keyalg EC -keypass $2 -storepass $2 -keystore $1.jks"
echo "keytool -export -alias $1 -storepass $2 -file $1.cer -keystore $1.jks"
