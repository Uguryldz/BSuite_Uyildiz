@echo off
REM ============================================================================
REM U YILDIZ BurpSuite Loader & Keygen — Windows build script
REM ============================================================================
REM Kullanim:  build.bat
REM Cikti:     build\uyildiz-burploader.jar
REM Gereksinim: JDK 17+ (java, javac, jar)
REM ============================================================================

setlocal
set ROOT=%~dp0
set SRC=%ROOT%src\main\java
set LIBS=%ROOT%libs
set BUILD=%ROOT%build
set OUT=%BUILD%\classes
set JAR_NAME=uyildiz-burploader.jar

echo [1/4] Temizleniyor...
if exist "%OUT%" rmdir /s /q "%OUT%"
if exist "%BUILD%\flat" rmdir /s /q "%BUILD%\flat"
if exist "%BUILD%\%JAR_NAME%" del /q "%BUILD%\%JAR_NAME%"
mkdir "%OUT%" 2>nul
mkdir "%BUILD%\flat" 2>nul

echo [2/4] ASM kutuphaneleri aciliyor (fat JAR icin)...
pushd "%BUILD%\flat"
jar xf "%LIBS%\asm-9.11.jar"
jar xf "%LIBS%\asm-tree-9.11.jar"
if exist module-info.class del /q module-info.class
if exist META-INF\MANIFEST.MF del /q META-INF\MANIFEST.MF
if exist META-INF\maven rmdir /s /q META-INF\maven
if exist META-INF\versions rmdir /s /q META-INF\versions
popd

echo [3/4] Kaynaklar derleniyor...
javac -cp "%BUILD%\flat" -d "%OUT%" "%SRC%\com\uyildiz\burploaderkeygen\*.java"
if errorlevel 1 (
   echo HATA: Derleme basarisiz!
   exit /b 1
)

echo [4/4] Fat JAR paketleniyor...
xcopy /e /i /y "%BUILD%\flat\org" "%OUT%\org" >nul
jar cfm "%BUILD%\%JAR_NAME%" "%ROOT%MANIFEST.MF" -C "%OUT%" .

echo.
echo === TAMAM ===
echo Cikti: %BUILD%\%JAR_NAME%
dir "%BUILD%\%JAR_NAME%" | findstr /r "[0-9]"
endlocal
