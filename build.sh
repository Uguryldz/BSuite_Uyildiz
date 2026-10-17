#!/usr/bin/env bash
# ============================================================================
# U YILDIZ BurpSuite Loader & Keygen — Linux/macOS build script
# ============================================================================
# Kullanim:   ./build.sh
# Cikti:      build/uyildiz-burploader.jar
# Gereksinim: JDK 17+ (java, javac, jar PATH icinde)
# ============================================================================
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src/main/java"
LIBS="$ROOT/libs"
BUILD="$ROOT/build"
OUT="$BUILD/classes"
FLAT="$BUILD/flat"
JAR_NAME="uyildiz-burploader.jar"

echo "[1/4] Temizleniyor..."
rm -rf "$OUT" "$FLAT" "$BUILD/$JAR_NAME"
mkdir -p "$OUT" "$FLAT"

echo "[2/4] ASM kutuphaneleri aciliyor (fat JAR icin)..."
( cd "$FLAT" && jar xf "$LIBS/asm-9.11.jar" && jar xf "$LIBS/asm-tree-9.11.jar" )
rm -f "$FLAT/module-info.class" "$FLAT/META-INF/MANIFEST.MF"
rm -rf "$FLAT/META-INF/maven" "$FLAT/META-INF/versions"

echo "[3/4] Kaynaklar derleniyor..."
javac -cp "$FLAT" -d "$OUT" "$SRC"/com/uyildiz/burploaderkeygen/*.java

echo "[4/4] Fat JAR paketleniyor..."
cp -r "$FLAT/org" "$OUT/"
jar cfm "$BUILD/$JAR_NAME" "$ROOT/MANIFEST.MF" -C "$OUT" .

echo
echo "=== TAMAM ==="
ls -lh "$BUILD/$JAR_NAME"
