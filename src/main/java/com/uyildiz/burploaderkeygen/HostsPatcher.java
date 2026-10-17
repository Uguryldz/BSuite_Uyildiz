// ============================================================================
// HostsPatcher.java  —  U YILDIZ · Hosts dosyasi duzenleyici
// ----------------------------------------------------------------------------
// Islev:
//   • Burp'un lisans sunucusuna (portswigger.net) phone-home yapmasini
//     engellemek icin hosts dosyasina 127.0.0.1 kaydi ekler.
//   • Satirlar "# U YILDIZ BEGIN" / "# U YILDIZ END" blogu icinde tutulur ki
//     temizlemek kolay olsun.
//   • Yonetici izni yoksa sessiz basarisiz olur (install() devam eder).
//
// Platform:
//   Windows: C:\Windows\System32\drivers\etc\hosts
//   Linux/macOS: /etc/hosts
//
// Blok formati:
//   # U YILDIZ BEGIN
//   127.0.0.1  portswigger.net
//   127.0.0.1  www.portswigger.net
//   127.0.0.1  portswigger-cdn.net
//   127.0.0.1  api.licensespring.com
//   # U YILDIZ END
// ============================================================================
package com.uyildiz.burploaderkeygen;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class HostsPatcher {
   private static final String BEGIN_TAG = "# U YILDIZ BEGIN";
   private static final String END_TAG   = "# U YILDIZ END";

   /** Engellenen host'lar — Burp phone-home / telemetry / lisans dogrulama.
    *  Ana amac: PortSwigger backend'ine "bu cihaz lisansli degil" sinyali
    *  gondermesini onlemek, cihaz imzasini (deviceId, installationId)
    *  ac1k etmemek. */
   private static final String[] HOSTS = {
      // Ana domain + CDN
      "portswigger.net",
      "www.portswigger.net",
      "portswigger-cdn.net",
      "www.portswigger-cdn.net",

      // Lisans / aktivasyon
      "license.portswigger.net",
      "licensing.portswigger.net",
      "activation.portswigger.net",
      "api.licensespring.com",
      "saas.licensespring.com",

      // Telemetri / crash raporu / kullanici analitiği
      "telemetry.portswigger.net",
      "analytics.portswigger.net",
      "crash.portswigger.net",
      "feedback.portswigger.net",
      "metrics.portswigger.net",

      // Guncelleme ve sürüm kontrolu
      "updates.portswigger.net",
      "releases.portswigger.net",
      "update.portswigger.net",
      "install.portswigger.net",

      // BApp Store (eklenti listesi — yuklu eklentileri sunucuya bildirir)
      "bappstore.portswigger.net",
      "store.portswigger.net",

      // Burp Collaborator (public server — kullanicinin kullandigini bildirir)
      "collaborator.portswigger.net",
      "polling.oastify.com",
      "oastify.com",

      // Browser indirmesi
      "browser.portswigger.net",
      "browser-updates.portswigger.net"
   };

   /** Platform'a gore hosts dosyasi yolu. */
   private static Path hostsFile() {
      String os = System.getProperty("os.name").toLowerCase();
      if (os.startsWith("win")) {
         String sysRoot = System.getenv("SystemRoot");
         if (sysRoot == null) sysRoot = "C:\\Windows";
         return Paths.get(sysRoot, "System32", "drivers", "etc", "hosts");
      }
      return Paths.get("/etc/hosts");
   }

   /** Hosts dosyasina U YILDIZ blogunu ekle (varsa guncelle).
    *  Yonetici izni yoksa false doner (hata firlatmaz). */
   public static boolean install() {
      Path hosts = hostsFile();
      if (!Files.exists(hosts)) return false;
      try {
         List<String> lines = new ArrayList<>(Files.readAllLines(hosts));
         // Varsa eski U YILDIZ blogunu kaldir
         lines = stripBlock(lines);
         // Yeni blogu ekle
         lines.add("");
         lines.add(BEGIN_TAG);
         for (String h : HOSTS) {
            lines.add("127.0.0.1  " + h);
         }
         lines.add(END_TAG);
         Files.write(hosts, lines);
         return true;
      } catch (IOException ex) {
         return false; // Yetki yok veya baska IO hatasi
      }
   }

   /** Hosts dosyasindan U YILDIZ blogunu kaldir. */
   public static boolean clean() {
      Path hosts = hostsFile();
      if (!Files.exists(hosts)) return false;
      try {
         List<String> lines = Files.readAllLines(hosts);
         List<String> cleaned = stripBlock(lines);
         if (cleaned.size() != lines.size()) {
            Files.write(hosts, cleaned);
         }
         return true;
      } catch (IOException ex) {
         return false;
      }
   }

   /** BEGIN/END arasindaki satirlari (ikisi dahil) listeden cikar. */
   private static List<String> stripBlock(List<String> lines) {
      List<String> out = new ArrayList<>();
      boolean inside = false;
      for (String line : lines) {
         String t = line.trim();
         if (t.equals(BEGIN_TAG)) { inside = true; continue; }
         if (t.equals(END_TAG))   { inside = false; continue; }
         if (!inside) out.add(line);
      }
      return out;
   }
}
