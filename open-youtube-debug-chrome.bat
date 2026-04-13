@echo off
taskkill /F /IM chrome.exe >nul 2>nul
taskkill /F /IM chromedriver.exe >nul 2>nul

if not exist "C:\Users\Inna\yt-debug-profile" mkdir "C:\Users\Inna\yt-debug-profile"

start "" "C:\Program Files\Google\Chrome\Application\chrome.exe" ^
  --remote-debugging-port=9222 ^
  --remote-debugging-address=127.0.0.1 ^
  --user-data-dir="C:\Users\Inna\yt-debug-profile"