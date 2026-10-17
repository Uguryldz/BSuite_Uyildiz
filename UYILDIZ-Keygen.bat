@echo off
REM ============================================================================
REM U YILDIZ Keygen baslatici
REM Hosts dosyasini duzenleyebilmek icin bu dosyaya sag tik -> "Yonetici
REM olarak calistir" demeniz onerilir.
REM ============================================================================
cd /d "%~dp0"

REM JAR'in konumu: ya ayni klasorde ya build\ altinda
set "JAR=uyildiz-burploader.jar"
if not exist "%JAR%" set "JAR=build\uyildiz-burploader.jar"
if not exist "%JAR%" (
    echo HATA: uyildiz-burploader.jar bulunamadi.
    echo Once build.bat ile derleyin.
    pause
    exit /b 1
)

REM Yonetici degilsek, kullaniciya hatirlatma yap
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo UYARI: Yonetici izni YOK.
    echo        "Network Kes" butonu calismayabilir.
    echo        Bu dosyaya sag tik -^> "Yonetici olarak calistir" deyin.
    echo.
    timeout /t 3 >nul
)

start "" javaw -jar "%JAR%"
