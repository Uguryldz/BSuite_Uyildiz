// ============================================================================
// Keygen.java  —  U YILDIZ Burp Loader Keygen
// ----------------------------------------------------------------------------
// Bu sinif Burp Suite icin lisans metni ve aktivasyon cevabi uretir.
// RSA ozel anahtarlar ile imzalama (SHA256withRSA + SHA1withRSA) ve DES ile
// sifreleme/sifre cozme islemlerini yapar. UI (KeygenForm) tarafindan cagrilir.
// ============================================================================
package com.uyildiz.burploaderkeygen;

// --- Standart kutuphaneler ---
import java.io.ByteArrayOutputStream;                 // Byte dizisi birikim akisi
import java.math.BigInteger;                          // Hex -> BigInteger donusumu
import java.security.GeneralSecurityException;        // Guvenlik hatalari ust sinifi
import java.security.KeyFactory;                      // RSA anahtar fabrikasi
import java.security.NoSuchAlgorithmException;        // Algoritma yok hatasi
import java.security.Signature;                       // Dijital imza API'si
import java.security.interfaces.RSAPrivateKey;        // RSA ozel anahtar arayuzu
import java.security.spec.InvalidKeySpecException;    // Hatali anahtar spec'i
import java.security.spec.PKCS8EncodedKeySpec;        // PKCS8 kodlu anahtar sarmali
import java.util.ArrayList;                           // Dinamik liste
import java.util.Base64;                              // Base64 enc/dec
import java.util.List;                                // Liste arayuzu
import java.util.Random;                              // Rastgele sayi uretici
import javax.crypto.Cipher;                           // DES sifreleme motoru
import javax.crypto.spec.SecretKeySpec;               // DES anahtar sarmali

public class Keygen {
   // 2048-bit RSA ozel anahtar (hex PKCS8) — SHA256withRSA imzasinda kullanilir
   private static final String privateKey2048 = "30820238020100300D06092A864886F70D0101010500048202223082021E02010002820101009D9DF9EB49890DE8F193C89598584BC947BA83727B2D89AA8BE3A4689130FE2E948967D40B656762F9989E59C9655E28E33FD4B4A544126FDD90A566BB61C2D7C74A6829265767B56E28FD2214D4BEB3B1DA4722BC394E2E6AFA0F1689FA9DB442643DDDA84997C5AD15B57EE5BD1A357CABF6ED4CAAA5FB8872E07C8F5FAE1C573C1214DD273C6D8887D7E993208D75118CC2305D60AA337B0999B69988322A8FAA9FBFF49AB70B71723E1CBD79D12640AF19E6FBC28C05E6630414DBAD9AEF912D0AC53E40B7F48EE29BFE1DEFCFB0BDB1B6C5BF8B06DCCA15FA1FC3F468952D481070C92C386D3CE6187B062038A6CA822D352ECEBEAC195918F9BB5C3AC3020100028201005DAD71C754BA3F692E835E1903259F4D6EF33C82C3110A9C316E47DDDA455B1D062D306787AA6A2B1A1B8A29E517F941A5E6DF1DCA87CDC96CCF366EFB799C1B31185915F3F2C8F1BD1A61706B1F1284AC7506087004432235748F991EC2B40E59D3482DC08294D0E9115900A5BCA1A21E89FA45896677262B2FD39A54805273162D655F1AB4392CE4E01A4DD63F7EF387B79D53B73BBE45EA7D9BE64A627CFB3DAE2843E85ED3697672BD4832F5EEB4C18C4D15FEB550E0B5A7018A3CD39A9FD4BDA35A6F88BD00CCBC787419AD57C54FA823EC3D7662710B03C2622E9E2DE546B21CA1C76672B1CC6BD92871A0F96051E31CB060E0DDB4022BEB2897A88761020100020100020100020100020100";
   // 1024-bit RSA ozel anahtar (hex PKCS8) — SHA1withRSA imzasinda kullanilir
   private static final String privateKey1024 = "30820136020100300D06092A864886F70D0101010500048201203082011C020100028181008D187233EB87AB60DB5BAE8453A7DE035428EB177EC8C60341CAB4CF487052751CA8AFF226EA3E98F0CEEF8AAE12E3716B8A20A24BDE20703865C9DBD9543F92EA6495763DFD6F7507B8607F2A14F52694BB9793FE12D3D9C5D1C0045262EA5E7FA782ED42568C6B7E31019FFFABAEFB79D327A4A7ACBD4D547ACB2DC9CD04030201000281807172A188DBAD977FE680BE3EC9E0E4E33A4D385208F0383EB02CE3DAF33CD520332DF362BA2588B58292710AC9D2882C4F329DF0C11DD66944FF9B21F98A031ED27C19FE2BCF8A09AD3E254A0FD7AB89E0D1E756BCF37ED24D42D1977EA7C1C78ABF4D13F752AE48B426A2DC98C5D13B2313609FAA6441E835DC61D17A01D1A9020100020100020100020100020100";
   // DES simetrik sifreleme icin kullanilan anahtar (hem lisans hem aktivasyon)
   private static final byte[] encryption_key = "burpr0x!".getBytes();

   /** Burp'tan gelen aktivasyon istegini isler ve geri gonderilecek cevabi uretir. */
   public static String generateActivation(String activationRequest) {
      // Istek metnini DES ile cozup NUL-ayrilmis alanlara parcala
      ArrayList<String> request = decodeActivationRequest(activationRequest);
      if (request == null) {
         // Cozme basarisizsa kullaniciya anlasilir mesaj dondur
         return "Error decoding activation request :-(";
      } else {
         // Cevap paketini sira ile olustur
         ArrayList<String> al = new ArrayList();
         al.add("0.4315672535134567");                 // Sabit sanal rastgele deger (Burp bu alani bekler)
         al.add(request.get(0));                       // Istege ait token (echo)
         al.add("activation");                         // Islem turu etiketi
         al.add(request.get(1));                       // Lisans adi (echo)
         al.add("True");                               // Aktivasyon basarili bayragi
         al.add("");                                   // Rezerve alan (bos)
         al.add(request.get(2));                       // Donanim/host bilgisi (echo)
         al.add(request.get(3));                       // Ek echo alani
         // Alanlarin RSA-SHA256 imzasi (uzun imza)
         al.add(getSign(privateKey2048, getSignatureBytes(al), "SHA256withRSA"));
         // Alanlarin RSA-SHA1 imzasi (eski Burp uyumlulugu)
         al.add(getSign(privateKey1024, getSignatureBytes(al), "SHA1withRSA"));
         // Son olarak: diziyi birlestir, sifrele ve Base64 ile dondur
         return prepareArray(al);
      }
   }

   /** Liste elemanlarini NUL byte ile birlestirip imzalanacak ham byte dizisi dondurur. */
   private static byte[] getSignatureBytes(List<String> list) {
      try {
         ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
         for(String s : list) {
            byteArray.write(s.getBytes()); // String'i byte olarak yaz
            byteArray.write(0);            // Alan ayirici: NUL
         }
         return byteArray.toByteArray();   // Birlesmis ham byte dizisi
      } catch (Exception var3) {
         var3.printStackTrace();
         throw new RuntimeException(var3);
      }
   }

   /** Verilen ozel anahtarla data'yi imzalar ve Base64 string olarak dondurur. */
   private static String getSign(String pri, byte[] data, String method) {
      try {
         Signature sign = Signature.getInstance(method);         // Imza algoritmasi secimi
         sign.initSign(getPriKeyByHex(pri));                     // Ozel anahtari yukle
         sign.update(data);                                      // Veriyi besle
         byte[] signature = sign.sign();                         // Imzayi uret
         return Base64.getEncoder().encodeToString(signature);   // Base64 olarak dondur
      } catch (Exception var5) {
         var5.printStackTrace();
         return null;
      }
   }

   /** Hex formatindaki ozel anahtari BigInteger uzerinden PKCS8 byte dizisine donusturur. */
   public static RSAPrivateKey getPriKeyByHex(String hexStr) {
      BigInteger hex = new BigInteger(hexStr, 16); // Hex -> BigInteger
      byte[] priData = hex.toByteArray();          // -> byte[]
      return getPriKeyByBytes(priData);            // -> RSAPrivateKey
   }

   /** PKCS8 kodlanmis byte dizisinden RSA ozel anahtar uretir. */
   public static RSAPrivateKey getPriKeyByBytes(byte[] priData) {
      try {
         KeyFactory keyFactory = KeyFactory.getInstance("RSA");                      // RSA fabrikasi
         PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(priData); // Spec sar
         return (RSAPrivateKey)keyFactory.generatePrivate(pkcs8EncodedKeySpec);      // Anahtari olustur
      } catch (InvalidKeySpecException | NoSuchAlgorithmException var3) {
         ((GeneralSecurityException)var3).printStackTrace();
         return null;
      }
   }

   /** Lisans adi girilince full lisans metni (Base64 blob) uretir. */
   public static String generateLicense(String licenseName) {
      ArrayList<String> al = new ArrayList();
      al.add(getRandomString());  // 32 karakterlik rastgele lisans ID
      al.add("license");           // Paket turu
      al.add(licenseName);         // Kullanici tarafindan girilen lisans sahibi
      al.add("4102415999000");    // Son kullanim: 2099-12-31 timestamp
      al.add("1");                 // Lisans sayisi
      al.add("full");              // "full" surum tipi
      // SHA256 ile uzun imza
      al.add(getSign(privateKey2048, getSignatureBytes(al), "SHA256withRSA"));
      // SHA1 ile ikinci imza
      al.add(getSign(privateKey1024, getSignatureBytes(al), "SHA1withRSA"));
      return prepareArray(al);     // Dizi -> encrypt -> Base64
   }

   /** 32 karakterlik buyuk harf + rakam karisimi lisans ID uretir. */
   private static String getRandomString() {
      String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"; // Izin verilen karakter kumesi
      StringBuilder str = new StringBuilder();
      Random rnd = new Random();                              // Rastgele kaynak
      while(str.length() < 32) {
         int index = (int)(rnd.nextFloat() * (float)CHARS.length()); // Rastgele indeks
         str.append(CHARS.charAt(index));                             // Karakteri ekle
      }
      return str.toString();
   }

   /** Alanlari NUL ile birlestir, DES ile sifrele, Base64 ile dondur. */
   private static String prepareArray(ArrayList<String> list) {
      try {
         ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
         // Son eleman haric NUL ile ayrilarak yaz
         for(int i = 0; i < list.size() - 1; ++i) {
            byteArray.write(((String)list.get(i)).getBytes());
            byteArray.write(0);
         }
         // Son elemani NUL koymadan yaz
         byteArray.write(((String)list.get(list.size() - 1)).getBytes());
         // DES ile sifrele ve Base64 ile paketle
         return new String(Base64.getEncoder().encode(encrypt(byteArray.toByteArray())));
      } catch (Exception var3) {
         var3.printStackTrace();
         throw new RuntimeException(var3);
      }
   }

   /** Verilen byte dizisini DES ile sifreler. */
   private static byte[] encrypt(byte[] arrayOfByte) {
      try {
         SecretKeySpec localSecretKeySpec = new SecretKeySpec(encryption_key, "DES"); // DES anahtari
         Cipher localCipher = Cipher.getInstance("DES");                              // DES motoru
         localCipher.init(Cipher.ENCRYPT_MODE, localSecretKeySpec);                   // Mod: SIFRELE
         return localCipher.doFinal(arrayOfByte);                                     // Sifrele
      } catch (Exception var4) {
         var4.printStackTrace();
         throw new RuntimeException(var4);
      }
   }

   /** Burp'tan gelen Base64/DES sifreli istegi parcalara ayirir. */
   private static ArrayList<String> decodeActivationRequest(String activationRequest) {
      try {
         ArrayList<String> ar = getParamsList(activationRequest);
         // Beklenen alan sayisi 5; degilse hatali girdi
         if (ar.size() != 5) {
            System.out.print("Activation Request Decoded to wrong size! The following was Decoded: \n");
            System.out.print(ar);
            return null;
         } else {
            return ar;
         }
      } catch (Exception var2) {
         var2.printStackTrace();
         return null;
      }
   }

   /** Base64 -> DES decrypt -> NUL ile ayrilmis alanlar listesi. */
   private static ArrayList<String> getParamsList(String data) {
      byte[] rawBytes = decrypt(Base64.getDecoder().decode(data)); // Decrypt
      ArrayList<String> ar = new ArrayList();
      int from = 0;
      // NUL (0x00) ayracina gore alanlari ayikla
      for(int i = 0; i < rawBytes.length; ++i) {
         if (rawBytes[i] == 0) {
            ar.add(new String(rawBytes, from, i - from));
            from = i + 1;
         }
      }
      // Son parcayi ekle
      ar.add(new String(rawBytes, from, rawBytes.length - from));
      return ar;
   }

   /** Verilen byte dizisini DES ile sifre cozer. */
   private static byte[] decrypt(byte[] arrayOfByte) {
      try {
         SecretKeySpec localSecretKeySpec = new SecretKeySpec(encryption_key, "DES"); // DES anahtari
         Cipher localCipher = Cipher.getInstance("DES");                              // DES motoru
         localCipher.init(Cipher.DECRYPT_MODE, localSecretKeySpec);                   // Mod: COZ
         return localCipher.doFinal(arrayOfByte);                                     // Sifre coz
      } catch (Exception var3) {
         var3.printStackTrace();
         throw new RuntimeException(var3);
      }
   }
}
