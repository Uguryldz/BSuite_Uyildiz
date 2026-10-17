# BSuite_Uyildiz

**U YILDIZ · BurpSuite Keygen Studio** — eğitim amaçlı Java agent + Swing UI.
Java Instrumentation API, bytecode manipülasyonu (ASM), RSA/DES
kriptografisi, install4j JVM launcher mekanikleri ve iki katmanlı
anti-telemetri savunması bir arada çalışır.

> ⚠️ **Yasal uyarı**
> Bu depo yalnızca **eğitim ve araştırma** amacıyla yayınlanmıştır.
> PortSwigger Ltd. lisans sözleşmesinin ihlali kullanıcının kendi
> sorumluluğundadır. Burp Suite Professional satın alın:
> <https://portswigger.net/burp/pro>.

---

## 🚀 Hızlı başlangıç

### 1) Derle

**Windows:**
```batch
build.bat
```

**Linux / macOS:**
```bash
chmod +x build.sh
./build.sh
```

Çıktı: `build/uyildiz-burploader.jar` (~223 KB fat JAR, ASM gömülü)

### 2) Yönetici olarak çalıştır

Hosts dosyası düzenlemesi için gerekli:

**Windows:** `UYILDIZ-Keygen.bat` → sağ tık → **"Yönetici olarak çalıştır"**

**PowerShell:**
```powershell
Start-Process java -ArgumentList '-jar','build\uyildiz-burploader.jar' -Verb RunAs
```

**Linux / macOS:**
```bash
sudo java -jar build/uyildiz-burploader.jar
```

### 3) Lisans akışı (2 buton + 4 adım)

Keygen UI'de sağ üstte:

| Buton | İşlev |
|-------|-------|
| **🚀 Burp Baslat** | Tek tık ile her şey (agent kopya + vmoptions sabitleme + hosts engel + telemetry kapat + Burp'u başlat) |
| **🧹 Lisans Temizle** | Registry'deki Burp lisansını siler (sıfırdan aktive etmek için) |

1. **🚀 Burp Baslat** → Burp açılır, "Enter your license key" ekranı gelir.
2. Keygen'de **"Lisans Sahibi"** alanına adını yaz.
3. **[1] Lisans** panelinden Base64 bloğunu **Kopyala**, Burp'a yapıştır → Next.
4. Burp "could not contact server" diyecek → **"Manual activation"** seç.
   Burp'un verdiği metni **[2] Aktivasyon İsteği**'ne **Yapıştır**.
   **[3] Aktivasyon Cevabı** otomatik dolar — Kopyala, Burp'a geri yapıştır → ✓

Aktivasyon tamamlanınca `BurpSuite.exe`'ye direkt çift tıklayınca da agent
otomatik yüklenir (çünkü `BurpSuite.vmoptions` sabitlendi ve salt-okunur
işaretlendi).

---

## 🛡️ Güvenlik / Anonimlik — İki katmanlı savunma

Burp'u pentest aracı olarak kullanacağın için **sadece PortSwigger'ın kendi
phone-home endpoint'leri** bloklanır. Burp'un test ettiğin HEDEF siteye
erişimi hiçbir şekilde etkilenmez.

### Katman 1 — Hosts dosyası (DNS seviyesinde, 22 domain)

Keygen "🚀 Burp Baslat" basılınca `C:\Windows\System32\drivers\etc\hosts`
dosyasına şu blok eklenir (yönetici izni gerekir):

```
# U YILDIZ BEGIN
127.0.0.1  portswigger.net
127.0.0.1  www.portswigger.net
127.0.0.1  portswigger-cdn.net
127.0.0.1  www.portswigger-cdn.net
127.0.0.1  license.portswigger.net
127.0.0.1  licensing.portswigger.net
127.0.0.1  activation.portswigger.net
127.0.0.1  api.licensespring.com
127.0.0.1  saas.licensespring.com
127.0.0.1  telemetry.portswigger.net
127.0.0.1  analytics.portswigger.net
127.0.0.1  crash.portswigger.net
127.0.0.1  feedback.portswigger.net
127.0.0.1  metrics.portswigger.net
127.0.0.1  updates.portswigger.net
127.0.0.1  releases.portswigger.net
127.0.0.1  update.portswigger.net
127.0.0.1  install.portswigger.net
127.0.0.1  bappstore.portswigger.net
127.0.0.1  store.portswigger.net
127.0.0.1  collaborator.portswigger.net
127.0.0.1  polling.oastify.com
127.0.0.1  oastify.com
127.0.0.1  browser.portswigger.net
127.0.0.1  browser-updates.portswigger.net
# U YILDIZ END
```

Böylece lisans doğrulama, telemetri, crash raporu, update kontrolü,
BApp Store ve Collaborator public server trafikleri DNS seviyesinde
127.0.0.1'e düşer.

### Katman 2 — Registry (uygulama ayarı, telemetri kapat)

`HKCU\Software\JavaSoft\Prefs\burp\...` altında:

| Anahtar | Değer | Etkisi |
|---------|-------|--------|
| `feedbackReportingEnabled` | `false` | Crash + kullanım raporu gönderilmez |
| `AUTO_UPDATE_CHANNEL`      | `Never` | Sürüm sorgusu yapılmaz |
| `collaborator.enabled`     | `false` | Public Collaborator server kullanılmaz |

### Sonuç

PortSwigger backend'ine şunlar **GİTMEZ**:
- Cihaz ID (`global.suite.deviceId`)
- Installation ID (`burp.suite.installationId`)
- Crash raporu / stack trace'ler
- Hangi eklentilerin yüklü olduğu
- Lisans doğrulama isteği (zaten agent sahte imzayı geçerli saydırıyor)
- Hangi target'ları test ettiğin (BApp Store sorguları yok)

Burp'un **asıl işlevi (pentest trafiği)** tamamen açık kalır — target
sitelere, intranet'e, upstream proxy'ye, Burp Browser'a hiç dokunulmaz.

---

## 🧩 Teknik mimari

```
┌────────────────────┐        ┌──────────────────────────┐
│ KeygenForm (Swing) │        │ Loader (Java agent)      │
│   • Lisans üretimi │        │   • premain + ASM patch  │
│   • Aktivasyon      │        │   • bigint_patch         │
│     istek/cevap     │        │   • burp_patch1/2        │
│   • Installer UI    │        │   • bounty_patch         │
└────────────────────┘        └──────────────────────────┘
          │                                 │
          ▼                                 ▼
┌────────────────────┐        ┌──────────────────────────┐
│ Installer          │        │ Filter (runtime hook)    │
│   • detect Burp    │        │   • sahte LicenseSpring  │
│   • copy agent     │        │     cevapları            │
│   • vmoptions +    │        │   • Burp lisans paketini │
│     read-only      │        │     açar/dönüştürür      │
│   • hosts engeli   │        └──────────────────────────┘
│   • telemetry kes  │
│   • launch Burp    │
└────────────────────┘
          │
          ├──► HostsPatcher     — 22 PortSwigger domain → 127.0.0.1
          ├──► PrivacyHardener  — telemetry/update/collaborator kapat
          └──► LicensePersister — registry lisans oku/yaz/sil
```

### Sınıflar

| Sınıf              | Rol |
|--------------------|-----|
| `KeygenForm`       | Dark-theme Swing UI (main entry), 2 buton + 3 panel |
| `Keygen`           | RSA (SHA256withRSA + SHA1withRSA) + DES simetrik şifreleme |
| `Loader`           | Java agent — ASM ile 4 bytecode patch |
| `Filter`           | Agent'ın yönlendirdiği runtime çağrıları karşılar |
| `Installer`        | Burp bul + agent kur + vmoptions sabitle + hosts/registry güvenlik + başlat |
| `HostsPatcher`     | hosts dosyasına BEGIN/END bloğu ekler/çıkarır |
| `PrivacyHardener`  | Burp telemetry/update/collaborator registry ayarlarını kapatır |
| `LicensePersister` | Burp registry'deki aktif lisansı oku/yaz/sil |

---

## 🧠 Nasıl çalışır? (öğrenme notları)

### A. Keygen (`Keygen.java`)

- Kullanıcının girdiği "licensed to X" metnine hard-coded iki RSA özel
  anahtarla (2048-bit + 1024-bit) iki imza (SHA256 + SHA1) eklenir.
- Alanlar `NUL` byte ile ayrılır → DES/ECB ile şifrelenir → Base64 ile
  paketlenir.
- Aynı mantık **aktivasyon cevabı** üretirken de kullanılır (ters taraf).

### B. Loader (`Loader.java`) — ASM bytecode patch'leri

Dört ayrı transformasyon JVM sınıfları yüklenirken uygulanır:

1. **`bigint_patch`** → `java.math.BigInteger.oddModPow` içine, Burp'un
   public RSA modulus'u ile karşılaştırma yapan branch enjekte eder;
   eşleşirse modulus'u kendi sahte modulus'umuzla değiştirir. Böylece
   Burp'un imza doğrulaması bizim özel anahtarımızı "resmi" sanar.

2. **`burp_patch1`** → Burp'un lisans doğrulama metodu
   (`([Object[], Object)V`, > 20000 instruction) komple silinir ve içeriği
   `Filter.BurpFilter(obj)` çağrısı ile değiştirilir.

3. **`burp_patch2`** → Yedek strateji: aynı metoddaki ikinci
   `new Exception()` dallanmasını koşulsuz GOTO'ya (opcode 167) çevirir.

4. **`bounty_patch`** → `feign/okhttp/OkHttpClient.toFeignResponse` HTTP
   yanıtını `Filter.BountyFilter(url, body)` ile değiştirir. Burp Bounty
   eklentisinin LicenseSpring API çağrılarını sahte "geçerli lisans"
   JSON'u ile karşılar.

### C. Modern JDK uyumluluğu (kritik!)

Burp installer'ı artık **OpenJDK 26** gömüyor; eski keygen'ler
`jdk.internal.org.objectweb.asm` paketini kullanıyordu, bu paket
JDK 24'te kaldırıldı. Bu proje:

1. Harici **ASM 9.11** (`org.ow2.asm`) kullanır, fat JAR içine gömülür.
2. ASM 9.11 JDK 25'e kadar destekler. JDK 26 class file **version 70**'yi
   tanımaz. Çözüm: `Loader.transform()` her sınıfın major version
   byte'ını (byte 6-7, big-endian) geçici olarak 69'a düşürür, patch
   sonrası orijinal 70'e geri yazar. Böylece JVM "bu Java 26 sınıfı"
   diye yükler, ASM "bu Java 25" diye işler.

### D. Install4j entegrasyonu

Install4j launcher (`BurpSuite.exe`) açılışta `BurpSuite.vmoptions`
dosyasını okur. "🚀 Burp Baslat" basılınca:

1. **Agent JAR** Burp klasörüne kopyalanır.
2. **`BurpSuite.vmoptions`** dosyasına `-javaagent:uyildiz-burploader.jar`
   satırı eklenir, dosya **salt-okunur (read-only)** işaretlenir ki
   install4j launcher yeniden yazamasın.
3. **Hosts dosyası** 22 PortSwigger domain için güncellenir.
4. **PrivacyHardener** registry telemetry anahtarlarını kapatır.
5. **Burp başlatılır**: gömülü JRE (`jre\bin\javaw.exe`) ile
   `-javaagent:uyildiz-burploader.jar -jar burpsuite.jar`.

### E. Registry'de lisans

Burp aktif lisansı Java `Preferences` API'siyle saklıyor:

- **Windows:** `HKCU\Software\JavaSoft\Prefs\burp\license1`
- **Linux:**   `~/.java/.userPrefs/burp/prefs.xml`
- **macOS:**   `~/Library/Preferences/com.apple.java.util.prefs.plist`

`LicensePersister` sınıfı bu değeri okur/yazar/siler. "🧹 Lisans Temizle"
butonu bu kaydı siler → Burp bir sonraki açılışta yeniden aktivasyon
ister.

---

## 📂 Dizin yapısı

```
BSuite_Uyildiz/
├── .gitignore
├── LICENSE                                    # MIT
├── MANIFEST.MF                                # Main-Class + Premain-Class
├── README.md                                  # Bu dosya
├── UYILDIZ-Keygen.bat                         # Windows yönetici launcher
├── build.bat                                  # Windows build
├── build.sh                                   # Linux/macOS build
├── libs/
│   ├── asm-9.11.jar                           # ow2 ASM core
│   └── asm-tree-9.11.jar                      # ow2 ASM tree API
└── src/main/java/com/uyildiz/burploaderkeygen/
    ├── KeygenForm.java                        # Swing UI (main)
    ├── Keygen.java                            # RSA + DES iş mantığı
    ├── Loader.java                            # Java agent + ASM patch
    ├── Filter.java                            # Runtime hook'lar
    ├── Installer.java                         # Kurulum/launch orchestrator
    ├── HostsPatcher.java                      # 22-domain phone-home engeli
    ├── PrivacyHardener.java                   # Telemetry/update/collaborator kapatıcı
    └── LicensePersister.java                  # Registry lisans okur/yazar
```

---

## 🔧 Gereksinimler

- **JDK 17 veya üstü** (derleme için — JDK 25/26 ile de uyumlu)
- **Java runtime 17+** (keygen UI için)
- **Burp Suite Professional (install4j installer)** — opsiyonel; sadece
  UI'yi test etmek için gerekli değildir.
- **Yönetici izni** (Windows: UAC) — hosts dosyasına yazabilmek için.

---

## 🐛 Sorun giderme

**"License key was not recognized" hatası:**
Agent yüklenmemiş. **"🚀 Burp Baslat"** butonunu kullan, `BurpSuite.exe`'ye
direkt çift tıklama. Veya aktivasyon sonrası `BurpSuite.exe` otomatik
agent yüklesin diye `BurpSuite.vmoptions`'un salt-okunur olarak
sabitlendiğinden emin ol.

**"Could not contact licensing server" + manual activation gelmiyor:**
Normal. **Yönetici** olarak çalıştır (`UYILDIZ-Keygen.bat` → sağ tık),
hosts dosyası düzenlenebilsin.

**Burp açıldığında agent banner'ı görünmüyor:**
PowerShell'den `BurpSuite.exe`'yi çalıştır, konsolda **U YILDIZ** banner
düşmeli:
```powershell
cd "C:\Users\<kullanici>\AppData\Local\Programs\BurpSuite"
.\jre\bin\java.exe -javaagent:uyildiz-burploader.jar -jar burpsuite.jar
```

**JAR yeniden derlenmiyor ("Device busy"):**
Keygen UI açıksa kapat. Veya:
```powershell
Get-Process java,javaw | Stop-Process -Force
```

**Hosts dosyası güncellenmedi:**
Yönetici değilsin. `UYILDIZ-Keygen.bat` → sağ tık → "Yönetici olarak
çalıştır". Yetkisiz çalıştırırsan agent + vmoptions + registry yine
çalışır ama hosts engeli eklenmez.

**Burp'u kaldırmak / crack'i geri almak istersem:**
`BurpSuite.vmoptions`'daki `-javaagent` satırını sil, dosyanın
read-only bayrağını kaldır. Hosts'tan `# U YILDIZ BEGIN` / `END`
arasındaki satırları sil. Registry'den
`HKCU\Software\JavaSoft\Prefs\burp\license1` değerini sil.

---

## 📜 Lisans

MIT — Ayrıntı için [LICENSE](LICENSE) dosyasına bakın.

## 🙏 Teşekkür

Bu proje, mevcut açık kaynaklı Burp loader/keygen çalışmalarından
esinlenerek **pedagojik amaçla yeniden yazılmış**, Türkçe satır
yorumlarıyla belgelenmiştir. Install4j entegrasyonu, modern JDK (24+)
uyumluluğu, iki katmanlı phone-home engeli ve registry lisans yönetimi
orijinal çalışmaların yapmadığı eklemelerdir.
