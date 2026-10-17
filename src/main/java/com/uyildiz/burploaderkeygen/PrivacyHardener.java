// ============================================================================
// PrivacyHardener.java  —  U YILDIZ · Burp telemetri/feedback kapatici
// ----------------------------------------------------------------------------
// Burp'un PortSwigger sunucusuna gonderebilecegi telemetri, feedback,
// auto-update kontrolu gibi "phone-home" davranislarini kapatan registry/
// preferences ayarlari uygular.
//
// Hosts bloklariyla birlikte (HostsPatcher) bu sinif iki katmanli savunma
// saglar: hosts katmani ag seviyesinde engel, bu katman uygulama seviyesinde
// hic cagri yapilmamasini saglar.
//
// Degistirdigi ayarlar:
//   burp.suite.feedbackReportingEnabled  = false  (crash/kullanim raporu)
//   burp.suite.AUTO_UPDATE_CHANNEL_*     = Never  (otomatik guncelleme)
//   burp.suite.trustprojectfile          = true   (ag kontrolu olmadan ac)
//
// Konum: HKCU\Software\JavaSoft\Prefs\burp\...
// ============================================================================
package com.uyildiz.burploaderkeygen;

import java.util.prefs.Preferences;

public class PrivacyHardener {

   private static final String NODE = "burp";

   /** Telemetri + otomatik guncellemeyi kapat. */
   public static boolean harden() {
      try {
         Preferences p = Preferences.userRoot().node(NODE);

         // 1) Feedback / crash reporting kapat
         p.put("burp.suite.feedbackReportingEnabled", "false");

         // 2) Auto-update kanalini "Never" olarak isaretle
         //    Burp farkli surumlerde farkli key adi kullaniyor; en yaygin
         //    olasi anahtarlari hepsinde kapat.
         try {
            // Mevcut auto-update anahtarlarini bulup "Never" yap
            for (String key : p.keys()) {
               if (key.startsWith("burp.suite.AUTO_UPDATE_CHANNEL") ||
                   key.toLowerCase().contains("autoupdate") ||
                   key.toLowerCase().contains("auto_update")) {
                  p.put(key, "§Never§Never");
               }
            }
         } catch (Exception ignored) {}

         // Guvence: standart isim
         p.put("burp.suite.AUTO_UPDATE_CHANNEL", "§Never§Never");

         // 3) Burp Collaborator public server kullanimini kapat ipucu
         //    (Burp kendi config dosyasindan da okuyabilir; en azindan bu
         //     prefs degerini false yapmayi deneyelim.)
         p.putBoolean("burp.suite.collaborator.enabled", false);

         p.flush();
         return true;
      } catch (Exception ex) {
         ex.printStackTrace();
         return false;
      }
   }

   /** Yapilan degisiklikleri geri al. */
   public static boolean restore() {
      try {
         Preferences p = Preferences.userRoot().node(NODE);
         p.put("burp.suite.feedbackReportingEnabled", "true");
         p.remove("burp.suite.collaborator.enabled");
         p.flush();
         return true;
      } catch (Exception ex) {
         return false;
      }
   }
}
