// ============================================================================
// LicensePersister.java  —  U YILDIZ · Burp lisansini kalicilestiren sinif
// ----------------------------------------------------------------------------
// Burp aktif lisansi Java Preferences API uzerinde "burp" node'u altinda
// "license1" anahtarinda saklar. Windows'ta bu, su registry yolunda tutulur:
//
//   HKEY_CURRENT_USER\Software\JavaSoft\Prefs\burp\license1
//
// Boylece Burp sunucuya baglanamadiginda bile buradan okur. Bu sinif:
//
//   • readLicense()  → mevcut kayitli lisansi okur
//   • saveLicense()  → keygen'den uretilen Base64 lisansi direkt kaydeder
//   • hasLicense()   → kayitli lisans var mi?
//   • removeLicense()→ kaydi tamamen siler
//
// Preferences API otomatik encode/decode yaptigi icin bize ekstra isleme
// (base64 kacirma, carekter escape, vb.) gerek yoktur.
// ============================================================================
package com.uyildiz.burploaderkeygen;

import java.util.prefs.Preferences;

public class LicensePersister {

   /** Burp'un Preferences node adi. */
   private static final String NODE = "burp";
   private static final String KEY  = "license1";

   /** Mevcut kayitli lisansi dondurur (yoksa null). */
   public static String readLicense() {
      try {
         Preferences p = Preferences.userRoot().node(NODE);
         return p.get(KEY, null);
      } catch (Exception ex) {
         return null;
      }
   }

   /** Verilen lisans string'ini Burp'un kullanici tercihleri kaydina yazar.
    *  Bu, agent kaldirilsa bile Burp'un lisansi gormeyi surdurmesini saglar.
    *  @return basarili olduysa true */
   public static boolean saveLicense(String license) {
      if (license == null) return false;
      String trimmed = license.trim();
      if (trimmed.isEmpty()) return false;
      try {
         Preferences p = Preferences.userRoot().node(NODE);
         p.put(KEY, trimmed);
         p.flush();                               // Diske/registry'ye yaz
         // Burp bazi surumlerde "license" key'ini de kontrol eder — yedek
         p.put("license", trimmed);
         p.flush();
         return true;
      } catch (Exception ex) {
         ex.printStackTrace();
         return false;
      }
   }

   /** Kayitli lisansi tamamen sil. */
   public static boolean removeLicense() {
      try {
         Preferences p = Preferences.userRoot().node(NODE);
         p.remove(KEY);
         p.remove("license");
         p.flush();
         return true;
      } catch (Exception ex) {
         return false;
      }
   }

   /** Kayitli lisans var mi? */
   public static boolean hasLicense() {
      String s = readLicense();
      return s != null && !s.trim().isEmpty();
   }
}
