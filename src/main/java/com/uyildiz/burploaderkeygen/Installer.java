// ============================================================================
// Installer.java  —  U YILDIZ · Otomatik Burp agent kurulumu
// ----------------------------------------------------------------------------
// Islev:
//   • Standart Burp kurulum dizinini bul (Windows / macOS / Linux)
//   • Mevcut uyildiz-burploader.jar'i o klasore kopyala
//   • user.vmoptions dosyasini olustur veya mevcut dosyaya gerekli satirlari
//     ekle (duplicate'siz)
//   • Install4j launcher otomatik olarak user.vmoptions okuyacagi icin
//     kullanicinin baska hicbir sey yapmasina gerek kalmaz.
// ============================================================================
package com.uyildiz.burploaderkeygen;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;

public class Installer {

   /** Kurulum sonucu: ne oldu, hangi klasor, hata varsa mesaj. */
   public static class Result {
      public final boolean success;
      public final String  message;   // kullaniciya gosterilecek kisa mesaj
      public final String  burpDir;   // null olabilir
      public Result(boolean s, String m, String d) { success = s; message = m; burpDir = d; }
   }

   /** Olasi Burp kurulum yollari — oncelik sirasina gore. */
   private static List<Path> candidates() {
      List<Path> list = new ArrayList<>();
      String os = System.getProperty("os.name").toLowerCase();
      String home = System.getProperty("user.home");

      if (os.startsWith("win")) {
         String localApp = System.getenv("LOCALAPPDATA");
         String programFiles = System.getenv("ProgramFiles");
         String programFilesX86 = System.getenv("ProgramFiles(x86)");
         if (localApp != null) list.add(Paths.get(localApp, "Programs", "BurpSuite"));
         if (localApp != null) list.add(Paths.get(localApp, "Programs", "BurpSuitePro"));
         if (programFiles != null) list.add(Paths.get(programFiles, "BurpSuitePro"));
         if (programFiles != null) list.add(Paths.get(programFiles, "BurpSuite"));
         if (programFilesX86 != null) list.add(Paths.get(programFilesX86, "BurpSuitePro"));
         if (programFilesX86 != null) list.add(Paths.get(programFilesX86, "BurpSuite"));
      } else if (os.contains("mac")) {
         list.add(Paths.get("/Applications/Burp Suite Professional.app/Contents/Resources"));
         list.add(Paths.get(home, "Applications/Burp Suite Professional.app/Contents/Resources"));
         list.add(Paths.get("/Applications/Burp Suite Community.app/Contents/Resources"));
      } else { // Linux
         list.add(Paths.get(home, "BurpSuitePro"));
         list.add(Paths.get(home, "BurpSuiteCommunity"));
         list.add(Paths.get("/opt/BurpSuitePro"));
         list.add(Paths.get("/opt/BurpSuiteCommunity"));
      }
      return list;
   }

   /** Burp klasoru oldugunu dogrulayan check — burpsuite.jar olmali. */
   private static boolean looksLikeBurp(Path dir) {
      if (!Files.isDirectory(dir)) return false;
      // burpsuite.jar veya BurpSuite.vmoptions yan yana olmali
      return Files.exists(dir.resolve("burpsuite.jar"))
          || Files.exists(dir.resolve("BurpSuite.vmoptions"))
          || Files.exists(dir.resolve("BurpSuite.exe"))
          || Files.exists(dir.resolve("BurpSuitePro.vmoptions"));
   }

   /** Olasi yollar arasindan ilk gercek Burp klasorunu dondurur. */
   public static Path detectBurpDir() {
      for (Path p : candidates()) {
         if (looksLikeBurp(p)) return p;
      }
      return null;
   }

   /** Calismakta olan JAR'in dosya yolunu dondurur. */
   public static Path selfJar() {
      try {
         return Paths.get(Installer.class.getProtectionDomain()
                .getCodeSource().getLocation().toURI());
      } catch (URISyntaxException e) {
         return null;
      }
   }

   /** Kurulum calistir: JAR'i kopyala + user.vmoptions duzenle. */
   public static Result install() {
      Path burpDir = detectBurpDir();
      if (burpDir == null) {
         return new Result(false, "Burp kurulumu bulunamadi. Lutfen BurpSuite'in standart yola kurulu oldugundan emin olun.", null);
      }

      Path self = selfJar();
      if (self == null || !Files.exists(self) || Files.isDirectory(self)) {
         return new Result(false, "Agent JAR dosyasi tespit edilemedi (IDE'den calistiriyor olabilirsiniz).", burpDir.toString());
      }

      String jarName = self.getFileName().toString();
      Path target = burpDir.resolve(jarName);

      try {
         // 1) Varsa eski read-only bayragini kaldir
         clearReadOnly(burpDir.resolve("BurpSuite.vmoptions"));
         clearReadOnly(burpDir.resolve("user.vmoptions"));

         // 2) JAR'i kopyala
         Files.copy(self, target, StandardCopyOption.REPLACE_EXISTING);

         // 3) BurpSuite.vmoptions'a -javaagent sabitle + read-only yap.
         //    Boylece BurpSuite.exe ile acildiginda bile agent yuklenir.
         hardenForExeLaunch();

         // 4) Hosts dosyasini duzenle (PortSwigger phone-home engeli)
         //    - Yetki yoksa sessiz devam; bu olmasa bile vmoptions yolu yeter
         boolean hostsOK = false;
         String hostsMsg = "";
         try {
            hostsOK = HostsPatcher.install();
            if (!hostsOK) hostsMsg = "\n⚠ Hosts dosyasi guncellenemedi (yonetici izni yok). Lisans dogrulama phone-home acik kalabilir.";
         } catch (Exception ex) { hostsMsg = "\n⚠ Hosts hatasi: " + ex.getMessage(); }

         // 5) Yedek: launcher batch dosyalari (install4j tamamen bozulursa diye)
         ensureLauncherBat(burpDir, jarName);

         // 6) user.vmoptions — install4j'nin yedek yolu olarak
         Path vmOpts = burpDir.resolve("user.vmoptions");
         List<String> required = new ArrayList<>();
         required.add("-javaagent:" + jarName);
         required.add("-Xverify:none");
         required.add("--add-opens=java.desktop/javax.swing=ALL-UNNAMED");
         required.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
         required.add("--add-opens=java.base/java.math=ALL-UNNAMED");

         List<String> existing = Files.exists(vmOpts) ? Files.readAllLines(vmOpts) : new ArrayList<String>();

         // Eski -javaagent satirlarini temizle (farkli isimdeki eski JAR'lara karsi)
         List<String> cleaned = new ArrayList<>();
         for (String line : existing) {
            String s = line.trim();
            if (s.startsWith("-javaagent:")) continue;      // eskiyi at
            cleaned.add(line);
         }

         // Eksik olanlari sona ekle
         for (String req : required) {
            boolean present = false;
            for (String line : cleaned) {
               if (line.trim().equals(req)) { present = true; break; }
            }
            if (!present) cleaned.add(req);
         }

         Files.write(vmOpts, cleaned);

         return new Result(true,
            "Kurulum basarili!\n\n" +
            "Yapilanlar:\n" +
            "  ✓ Agent JAR Burp klasorune kopyalandi\n" +
            "  ✓ BurpSuite.vmoptions'a -javaagent eklendi\n" +
            "  ✓ vmoptions salt-okunur yapildi (launcher bozamayacak)\n" +
            (hostsOK ? "  ✓ Hosts dosyasina phone-home engeli eklendi\n"
                     : "  — Hosts guncellenemedi (yonetici izni gerekli)\n") +
            "  ✓ Yedek launcher batch dosyalari olusturuldu\n\n" +
            "Klasor:\n  " + burpDir + "\n\n" +
            "Artik BurpSuite.exe'ye cift tiklasaniz bile agent otomatik\n" +
            "yuklenir. Normal akis: BurpSuite'i acin, lisansi yapistirin\n" +
            "(Keygen UI'den [1] panelinden kopyalayin)." + hostsMsg,
            burpDir.toString());

      } catch (IOException ex) {
         return new Result(false, "Kurulum sirasinda hata: " + ex.getMessage(), burpDir.toString());
      }
   }

   /** TEK TIKLA BURP BASLAT:
    *    1) Agent JAR'i yoksa Burp klasorune kopyalar
    *    2) Hosts'a phone-home engeli ekler (yetki varsa; yoksa gec)
    *    3) Burp'un gomulu JRE'siyle burpsuite.jar'i -javaagent ile baslatir
    *  Boylece kullanici tek butona basar, Burp agent ile acilir, lisans
    *  ekraninda manuel aktivasyona hazir olur.
    */
   public static Result runBurpExe() {
      Path burpDir = detectBurpDir();
      if (burpDir == null) {
         return new Result(false,
            "BurpSuite kurulumu bulunamadi.\n\n" +
            "Standart yollar:\n" +
            "  Windows: %LOCALAPPDATA%\\Programs\\BurpSuite\n" +
            "           %ProgramFiles%\\BurpSuitePro\n" +
            "  macOS:   /Applications/Burp Suite ....app\n" +
            "  Linux:   ~/BurpSuitePro, /opt/BurpSuitePro",
            null);
      }

      // 1) Agent JAR'i Burp klasorune kopyala (yoksa veya eskiyse)
      Path self = selfJar();
      String jarName = (self != null) ? self.getFileName().toString() : "uyildiz-burploader.jar";
      Path agentJar = burpDir.resolve(jarName);
      if (self != null && Files.exists(self) && !Files.isDirectory(self)) {
         try { Files.copy(self, agentJar, StandardCopyOption.REPLACE_EXISTING); }
         catch (IOException ignored) { /* varolan JAR yeter */ }
      }
      if (!Files.exists(agentJar)) {
         return new Result(false,
            "Agent JAR Burp klasorune kopyalanamadi:\n  " + agentJar,
            burpDir.toString());
      }

      // 2) BurpSuite.vmoptions'a -javaagent sabit yaz + read-only yap.
      //    Boylece BurpSuite.exe (install4j launcher) ile acildiginda da
      //    agent yuklenir, "unrecognized license" problemi olmaz.
      try { hardenForExeLaunch(); } catch (Exception ignored) {}

      // 3) Hosts dosyasina phone-home engeli (yetki yoksa sessiz gec)
      try { HostsPatcher.install(); } catch (Exception ignored) {}

      // 4) Burp telemetri + feedback + auto-update kapat (ag katmanindan sonra
      //    uygulama katmaninda da savunma; cihaz imzamizi acik etmemek icin)
      try { PrivacyHardener.harden(); } catch (Exception ignored) {}

      // 3) Burp'un gomulu JRE'sini kullanarak burpsuite.jar'i -javaagent ile ac
      Path burpJar = burpDir.resolve("burpsuite.jar");
      if (!Files.exists(burpJar)) {
         return new Result(false, "burpsuite.jar bulunamadi: " + burpJar, burpDir.toString());
      }

      String os = System.getProperty("os.name").toLowerCase();
      String javaExe = os.startsWith("win") ? "javaw.exe" : "java";
      Path java = burpDir.resolve("jre").resolve("bin").resolve(javaExe);
      if (!Files.exists(java)) {
         java = Paths.get(System.getProperty("java.home"), "bin", javaExe);
      }

      List<String> cmd = new ArrayList<>();
      cmd.add(java.toString());
      cmd.add("-javaagent:" + jarName);
      cmd.add("-Xverify:none");
      cmd.add("-XX:MaxRAMPercentage=50");
      cmd.add("--add-opens=java.desktop/javax.swing=ALL-UNNAMED");
      cmd.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
      cmd.add("--add-opens=java.base/java.math=ALL-UNNAMED");
      cmd.add("-jar");
      cmd.add("burpsuite.jar");

      try {
         ProcessBuilder pb = new ProcessBuilder(cmd);
         pb.directory(burpDir.toFile());
         pb.start();
         return new Result(true,
            "BurpSuite agent ile baslatildi.\n\n" +
            "Lisans ekraninda [1] panelindeki Base64 metnini\n" +
            "yapistirin → Next → 'Manual activation'.",
            burpDir.toString());
      } catch (IOException ex) {
         return new Result(false, "Baslatma hatasi: " + ex.getMessage(), burpDir.toString());
      }
   }

   /** Install4j launcher'i bypass eden Burp baslatici (.bat/.sh) olustur.
    *  Bu dosya gomulu JRE'yi direkt cagirir ve agent'i KESIN yukler.
    *  Kullanici BurpSuite.exe yerine BURP_UYILDIZ.bat/.sh calistirir.
    */
   private static void ensureLauncherBat(Path burpDir, String jarName) throws IOException {
      String os = System.getProperty("os.name").toLowerCase();

      if (os.startsWith("win")) {
         // Windows .bat
         Path bat = burpDir.resolve("BURP_UYILDIZ.bat");
         StringBuilder sb = new StringBuilder();
         sb.append("@echo off\r\n");
         sb.append("REM ============================================================\r\n");
         sb.append("REM U YILDIZ BurpSuite launcher — install4j bypass\r\n");
         sb.append("REM ============================================================\r\n");
         sb.append("cd /d \"%~dp0\"\r\n");
         sb.append("start \"BurpSuite (U YILDIZ)\" \"%~dp0jre\\bin\\javaw.exe\" ^\r\n");
         sb.append("    -javaagent:").append(jarName).append(" ^\r\n");
         sb.append("    -Xverify:none ^\r\n");
         sb.append("    -XX:MaxRAMPercentage=50 ^\r\n");
         sb.append("    --add-opens=java.desktop/javax.swing=ALL-UNNAMED ^\r\n");
         sb.append("    --add-opens=java.base/java.lang=ALL-UNNAMED ^\r\n");
         sb.append("    --add-opens=java.base/java.math=ALL-UNNAMED ^\r\n");
         sb.append("    -jar burpsuite.jar\r\n");
         Files.write(bat, sb.toString().getBytes("UTF-8"));

         // Debug surum (konsol kapanmaz, cikti goruntulenir)
         Path batDebug = burpDir.resolve("BURP_UYILDIZ_DEBUG.bat");
         StringBuilder sd = new StringBuilder();
         sd.append("@echo off\r\n");
         sd.append("REM Konsollu surum - U YILDIZ banner'ini ve hatalari gorebilmek icin\r\n");
         sd.append("cd /d \"%~dp0\"\r\n");
         sd.append("\"%~dp0jre\\bin\\java.exe\" ^\r\n");
         sd.append("    -javaagent:").append(jarName).append(" ^\r\n");
         sd.append("    -Xverify:none ^\r\n");
         sd.append("    -XX:MaxRAMPercentage=50 ^\r\n");
         sd.append("    --add-opens=java.desktop/javax.swing=ALL-UNNAMED ^\r\n");
         sd.append("    --add-opens=java.base/java.lang=ALL-UNNAMED ^\r\n");
         sd.append("    --add-opens=java.base/java.math=ALL-UNNAMED ^\r\n");
         sd.append("    -jar burpsuite.jar\r\n");
         sd.append("pause\r\n");
         Files.write(batDebug, sd.toString().getBytes("UTF-8"));

      } else {
         // Linux/macOS .sh
         Path sh = burpDir.resolve("burp_uyildiz.sh");
         StringBuilder sb = new StringBuilder();
         sb.append("#!/usr/bin/env bash\n");
         sb.append("# U YILDIZ BurpSuite launcher — install4j bypass\n");
         sb.append("cd \"$(dirname \"$0\")\"\n");
         sb.append("exec ./jre/bin/java \\\n");
         sb.append("    -javaagent:").append(jarName).append(" \\\n");
         sb.append("    -Xverify:none \\\n");
         sb.append("    -XX:MaxRAMPercentage=50 \\\n");
         sb.append("    --add-opens=java.desktop/javax.swing=ALL-UNNAMED \\\n");
         sb.append("    --add-opens=java.base/java.lang=ALL-UNNAMED \\\n");
         sb.append("    --add-opens=java.base/java.math=ALL-UNNAMED \\\n");
         sb.append("    -jar burpsuite.jar\n");
         Files.write(sh, sb.toString().getBytes("UTF-8"));
         try { sh.toFile().setExecutable(true, false); } catch (Exception ignored) {}
      }
   }

   /** Kurulumu kaldir: JAR'i sil ve vmoptions'tan -javaagent satirini cikar.
    *  Burp'un KAYITLI lisansi silinmez (Burp kendi config'inde tutar);
    *  sadece bizim agent + batch + vmoptions degisiklikleri geri alinir.
    */
   public static Result uninstall() {
      Path burpDir = detectBurpDir();
      if (burpDir == null) {
         return new Result(false, "Burp kurulumu bulunamadi.", null);
      }
      Path self = selfJar();
      String jarName = (self != null) ? self.getFileName().toString() : "uyildiz-burploader.jar";
      Path target = burpDir.resolve(jarName);
      Path vmOpts = burpDir.resolve("user.vmoptions");

      try {
         // Dosya izinlerini normale cevir (install sirasinda read-only yapmis olabiliriz)
         clearReadOnly(burpDir.resolve("BurpSuite.vmoptions"));
         clearReadOnly(vmOpts);

         Files.deleteIfExists(target);
         Files.deleteIfExists(burpDir.resolve("BURP_UYILDIZ.bat"));
         Files.deleteIfExists(burpDir.resolve("BURP_UYILDIZ_DEBUG.bat"));
         Files.deleteIfExists(burpDir.resolve("burp_uyildiz.sh"));

         // vmoptions'tan -javaagent satirlarini cikar
         if (Files.exists(vmOpts)) {
            List<String> lines = Files.readAllLines(vmOpts);
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
               String s = line.trim();
               if (s.startsWith("-javaagent:")) continue;
               cleaned.add(line);
            }
            Files.write(vmOpts, cleaned);
         }
         Path sysVm = burpDir.resolve("BurpSuite.vmoptions");
         if (Files.exists(sysVm)) {
            List<String> lines = Files.readAllLines(sysVm);
            List<String> cleaned = new ArrayList<>();
            for (String line : lines) {
               String s = line.trim();
               if (s.startsWith("-javaagent:")) continue;
               cleaned.add(line);
            }
            Files.write(sysVm, cleaned);
         }

         return new Result(true, "Kurulum kaldirildi. Burp artik agent olmadan calisacak.", burpDir.toString());
      } catch (IOException ex) {
         return new Result(false, "Kaldirma sirasinda hata: " + ex.getMessage(), burpDir.toString());
      }
   }

   /** Crack dosyalarini sil ama Burp lisansini bozma.
    *  Bu, uninstall() ile ayni; isim farkli cunku UI'de ayri bir dugme olarak
    *  "crack temizle" semantigiyle sunulur. hosts entry'leri de temizlenir.
    */
   public static Result cleanCrack() {
      Result r = uninstall();
      if (!r.success) return r;
      // Hosts dosyasindaki PortSwigger blok'lari temizle (yetki yoksa sessiz gec)
      try { HostsPatcher.clean(); } catch (Exception ignored) {}
      boolean hasLicense = LicensePersister.hasLicense();
      return new Result(true,
         "Crack temizlendi.\n\n" +
         "✓ Agent JAR, batch, vmoptions degisiklikleri silindi\n" +
         "✓ Hosts dosyasindaki PortSwigger engelleri temizlendi\n" +
         (hasLicense
            ? "✓ Burp registry lisansi KORUNDU — Burp bu lisansi okumayi\n  surdurecek\n"
            : "⚠ Burp registry'de kayitli lisans bulunamadi — once UI'de\n  '💾 Burp'a Kaydet' yaptiginizdan emin olun\n") +
         "\nOnemli: Phone-home engeli yok artik, Burp sunucuya baglanirsa\n" +
         "lisansi 'invalid' sayabilir. Internet'siz kullanmak daha guvenli.",
         r.burpDir);
   }

   /** Dosya salt-okunur (read-only) bayragini kaldir. */
   private static void clearReadOnly(Path p) {
      try {
         if (Files.exists(p)) p.toFile().setWritable(true, false);
      } catch (Exception ignored) {}
   }

   /** Dosyayi salt-okunur yap (install4j'nin yeniden yazmasini engellemek icin). */
   private static void setReadOnly(Path p) {
      try {
         if (Files.exists(p)) p.toFile().setReadOnly();
      } catch (Exception ignored) {}
   }

   /** install() icinde cagrilir — BurpSuite.vmoptions'a -javaagent ekler
    *  ve dosyayi salt-okunur yapar ki install4j launcher bir dahaki acilsta
    *  silmesin veya uzerine yazmasin.
    */
   public static Result hardenForExeLaunch() {
      Path burpDir = detectBurpDir();
      if (burpDir == null) return new Result(false, "Burp bulunamadi.", null);
      Path self = selfJar();
      String jarName = (self != null) ? self.getFileName().toString() : "uyildiz-burploader.jar";
      Path sysVm = burpDir.resolve("BurpSuite.vmoptions");

      try {
         clearReadOnly(sysVm);

         List<String> lines;
         if (Files.exists(sysVm)) {
            lines = new ArrayList<>(Files.readAllLines(sysVm));
         } else {
            lines = new ArrayList<>();
            lines.add("# U YILDIZ: install4j launcher bu dosyayi okur. SALT-OKUNUR yapildi.");
            lines.add("-XX:MaxRAMPercentage=50");
         }

         // Mevcut -javaagent satirlarini ve bizim eski satirlarimizi temizle
         List<String> cleaned = new ArrayList<>();
         for (String line : lines) {
            String s = line.trim();
            if (s.startsWith("-javaagent:")) continue;
            if (s.startsWith("-Xverify:none")) continue;
            if (s.contains("java.base/java.math")) continue;
            cleaned.add(line);
         }

         // Agent'imizi ve add-opens'leri ekle
         cleaned.add("-javaagent:" + jarName);
         cleaned.add("-Xverify:none");
         cleaned.add("--add-opens=java.desktop/javax.swing=ALL-UNNAMED");
         cleaned.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
         cleaned.add("--add-opens=java.base/java.math=ALL-UNNAMED");
         // include-options eklemeyi atla; direkt BurpSuite.vmoptions'da olsun

         Files.write(sysVm, cleaned);
         setReadOnly(sysVm);  // ← kritik: launcher yeniden yazamasın

         return new Result(true,
            "BurpSuite.vmoptions sabitlendi.\n\n" +
            "Dosya salt-okunur isaretlendi; install4j launcher yeniden\n" +
            "yazamayacak. Artik BurpSuite.exe ile acildiginda bile agent\n" +
            "yuklenecektir.",
            burpDir.toString());
      } catch (IOException ex) {
         return new Result(false, "Hata: " + ex.getMessage(), burpDir.toString());
      }
   }
}
