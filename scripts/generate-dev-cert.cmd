@echo off
REM Creates the self-signed PKCS12 keystore used by the dev profile (TLS on https://localhost:8443).
REM Usage: set APP_SSL_KEYSTORE_PASSWORD=<password> && scripts\generate-dev-cert.cmd
setlocal

if "%APP_SSL_KEYSTORE_PASSWORD%"=="" (
  echo APP_SSL_KEYSTORE_PASSWORD is required
  exit /b 1
)

if "%APP_SSL_KEYSTORE%"=="" set APP_SSL_KEYSTORE=.\data\dev-keystore.p12
for %%F in ("%APP_SSL_KEYSTORE%") do if not exist "%%~dpF" mkdir "%%~dpF"

keytool -genkeypair ^
  -alias real-estate-dev ^
  -keyalg RSA -keysize 2048 ^
  -storetype PKCS12 ^
  -keystore "%APP_SSL_KEYSTORE%" ^
  -storepass "%APP_SSL_KEYSTORE_PASSWORD%" ^
  -validity 365 ^
  -dname "CN=localhost, OU=dev, O=Constantino Imoveis" ^
  -ext "SAN=dns:localhost,ip:127.0.0.1"

echo Keystore written to %APP_SSL_KEYSTORE%
endlocal
