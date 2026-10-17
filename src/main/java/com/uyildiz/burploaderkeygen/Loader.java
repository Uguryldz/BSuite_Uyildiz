// ============================================================================
// Loader.java  —  U YILDIZ Burp Loader Keygen
// ----------------------------------------------------------------------------
// Java agent (javaagent:...) ile Burp JVM'ine yuklenen sinif.
// ClassFileTransformer uygulayarak Burp sinif bytecode'larini calisma aninda
// ASM ile duzenler:
//   - bigint_patch   : java.math.BigInteger.oddModPow icinde ozel bir sayiyi degistirir
//   - bounty_patch   : feign/okhttp cagrilarinin cevabini Filter.BountyFilter'a yonlendirir
//   - burp_patch1/2  : Burp'un buyuk lisans dogrulama metodunu Filter.BurpFilter'a yonlendirir
// ============================================================================
package com.uyildiz.burploaderkeygen;

// --- Java agent ve ASM (Maven Central ow2 ASM — modern Java'da jdk.internal.* yok) ---
import java.lang.instrument.ClassFileTransformer;              // Instrumentation API
import java.lang.instrument.Instrumentation;                   // Agent giris nesnesi
import java.security.ProtectionDomain;                         // Transform parametresi
import org.objectweb.asm.ClassReader;                          // Sinif okuyucu
import org.objectweb.asm.ClassWriter;                          // Sinif yazici
import org.objectweb.asm.tree.AbstractInsnNode;                // Soyut talimat dugumu
import org.objectweb.asm.tree.ClassNode;                       // Agac modu sinif
import org.objectweb.asm.tree.InsnList;                        // Talimat listesi
import org.objectweb.asm.tree.InsnNode;                        // Opcode talimati
import org.objectweb.asm.tree.IntInsnNode;                     // Int tasiyan talimat
import org.objectweb.asm.tree.JumpInsnNode;                    // Dallanma talimati
import org.objectweb.asm.tree.LabelNode;                       // Etiket dugumu
import org.objectweb.asm.tree.LdcInsnNode;                     // Sabit yukleyici (LDC)
import org.objectweb.asm.tree.MethodInsnNode;                  // Metot cagri talimati
import org.objectweb.asm.tree.MethodNode;                      // Agac modu metot
import org.objectweb.asm.tree.TypeInsnNode;                    // Tip tasiyan talimat (NEW vb.)
import org.objectweb.asm.tree.VarInsnNode;                     // Lokal degisken talimati

public class Loader implements ClassFileTransformer {
   /** Yapici: baslangic banner mesajini yazar. */
   public Loader() {
      // U YILDIZ banner (konsola yazilir)
      System.out.println("    __  __       __  __ ___  __    ____  ___  _____ \n" +
                         "   / / / /______/ /_/ // _ \\/ /   / __ \\/ _ \\/_   _|\n" +
                         "  / / / /______/ __/ // // / /   / / / / // /  / /  \n" +
                         " / /_/ /______/ /_/ // // / /___/ /_/ // // /  / /  \n" +
                         " \\____/       \\__/_//___/_____/\\____//___/   /_/   \n" +
                         "        U  Y I L D I Z   —   Burp Loader Keygen       ");
      System.out.println("UI: U YILDIZ sinifi tarafindan hazirlandi.");
   }

   /** burp_patch2: cok buyuk Burp sinifindaki ikinci "new Exception" dallanmasini ac. */
   public byte[] burp_patch2(String className, byte[] classBytes) {
      // Sadece burp/... paketindeki buyuk siniflar hedeflenir
      if (className.startsWith("burp/") && classBytes.length >= 110000) {
         ClassReader cr = new ClassReader(classBytes); // Byte dizisini oku
         ClassNode cn = new ClassNode();               // Agac moduna cevir
         cr.accept(cn, 0);

         // Hedef metot: ([Object[], Object)V ve >20000 instr
         for(MethodNode method : cn.methods) {
            if (method.desc.equals("([Ljava/lang/Object;Ljava/lang/Object;)V") && method.instructions.size() > 20000) {
               InsnList insnList = method.instructions;
               int j = 0;
               // Sondan basa dogru NEW java/lang/Exception dugumlerini tara
               for(int i = insnList.size() - 1; i > 0; --i) {
                  if (insnList.get(i) instanceof TypeInsnNode) {
                     TypeInsnNode typeInsnNode = (TypeInsnNode)insnList.get(i);
                     // NEW opcode == 187
                     if (typeInsnNode.getOpcode() == 187 && "java/lang/Exception".equals(typeInsnNode.desc)) {
                        ++j;
                        // Ikinci NEW Exception bulundugunda yakinindaki dallanmayi GOTO yap
                        if (j == 2) {
                           for(int k = 0; k < 6; ++k) {
                              if (insnList.get(i - k) instanceof JumpInsnNode) {
                                 JumpInsnNode jumpInsnNode = (JumpInsnNode)insnList.get(i - k);
                                 // 167 == GOTO; sart dallanmasini kosulsuz atlamaya cevir
                                 method.instructions.insert(insnList.get(i - k), new JumpInsnNode(167, jumpInsnNode.label));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         ClassWriter writer = new ClassWriter(3); // ComputeFrames + ComputeMaxs
         cn.accept(writer);
         return writer.toByteArray();
      } else {
         return null; // Hedef degil
      }
   }

   /** bounty_patch: feign/okhttp cagrisinin donus body'sini Filter.BountyFilter ile degistir. */
   public byte[] bounty_patch(String className, byte[] classBytes) {
      // Sadece feign/okhttp/OkHttpClient hedef
      if (!className.equals("feign/okhttp/OkHttpClient")) {
         return null;
      } else {
         ClassReader cr = new ClassReader(classBytes);
         ClassNode cn = new ClassNode();
         cr.accept(cn, 0);

         // toFeignResponse(okhttp3.Response, feign.Request) metodunu bul
         for(MethodNode method : cn.methods) {
            if ("toFeignResponse".equals(method.name) && "(Lokhttp3/Response;Lfeign/Request;)Lfeign/Response;".equals(method.desc)) {
               InsnList srcList = method.instructions;
               AbstractInsnNode[] insnNodes = srcList.toArray();

               // Response.Builder.build() cagrisini yakalayip oncesine filtre enjekte et
               for(AbstractInsnNode insnNode : insnNodes) {
                  if (insnNode instanceof MethodInsnNode && insnNode.getOpcode() == 182) { // INVOKEVIRTUAL
                     MethodInsnNode methodInsnNode = (MethodInsnNode)insnNode;
                     if (methodInsnNode.owner.equals("feign/Response$Builder") && methodInsnNode.name.equals("build") && methodInsnNode.desc.equals("()Lfeign/Response;")) {
                        InsnList insnList = new InsnList();
                        // Request.url() cagrisi
                        insnList.add(new VarInsnNode(25, 1));
                        insnList.add(new MethodInsnNode(182, "feign/Request", "url", "()Ljava/lang/String;", false));
                        // Request.body() cagrisi
                        insnList.add(new VarInsnNode(25, 1));
                        insnList.add(new MethodInsnNode(182, "feign/Request", "body", "()[B", false));
                        // Filter.BountyFilter(url, body) cagrisi (static)
                        insnList.add(new MethodInsnNode(184, "com/uyildiz/burploaderkeygen/Filter", "BountyFilter", "(Ljava/lang/String;[B)[B", false));
                        // Sonucu lokal 2'ye kaydet
                        insnList.add(new VarInsnNode(58, 2));
                        insnList.add(new VarInsnNode(25, 2));
                        // null ise atla (orijinal cevap kullanilsin)
                        LabelNode outLabel = new LabelNode();
                        insnList.add(new JumpInsnNode(198, outLabel)); // IFNULL
                        insnList.add(new VarInsnNode(25, 2));
                        // Response.Builder.body(byte[]) cagrisi
                        insnList.add(new MethodInsnNode(182, "feign/Response$Builder", "body", "([B)Lfeign/Response$Builder;", false));
                        // Status 200 ayarla
                        insnList.add(new IntInsnNode(17, 200));
                        insnList.add(new MethodInsnNode(182, "feign/Response$Builder", "status", "(I)Lfeign/Response$Builder;", false));
                        insnList.add(outLabel);
                        srcList.insertBefore(methodInsnNode, insnList);
                     }
                  }
               }
            }
         }

         ClassWriter writer = new ClassWriter(3);
         cn.accept(writer);
         return writer.toByteArray();
      }
   }

   /** bigint_patch: java/math/BigInteger.oddModPow icinde belirli bir modulusu degistirir. */
   public byte[] bigint_patch(String className, byte[] classBytes) {
      if (!className.equals("java/math/BigInteger")) {
         return null;
      } else {
         try {
            ClassReader reader = new ClassReader(classBytes);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            // oddModPow(BigInteger, BigInteger) metodunu bul
            for(MethodNode mn : node.methods) {
               if ("oddModPow".equals(mn.name) && "(Ljava/math/BigInteger;Ljava/math/BigInteger;)Ljava/math/BigInteger;".equals(mn.desc)) {
                  InsnList instructions = new InsnList();
                  // Burp public RSA modulusunu LDC ile stack'e koy
                  instructions.add(new LdcInsnNode("784494550650629402578318812038873246103062429392765900863679692401515917483288444551360396993891243581323776031174104716520618196517054075399181152805062167863399993397788108526062574442957397380276947009110814837620187732300913174633871838721345699310979475106228455540662931554176848428161559382213664590949585029191542943085957823265503304336742766067624625736735254354398945046006311244707353417137449868938968730226771591246625865947893595622114503547108373009472960599444147060111971350419883686667465293379976619008764355965580465504280432683151160078556566540083447715822702727667177022111431814347374423107231482768994558297231501807470318213348545623616323683043933536716592761352026200587983508688675808140761791235890894421345168268944272003766511723863509248901907706583412647586304919169213119463168988654772466925970559627610723088498856098005959331457878486945150019623509488580895386399262569844097131075786500942530443065538693113151751368621487717116172049865065084334155464496258411943450302953590604940950912787049251686884076034812543594664806687830000981320979025230888404872529342183959392375396232582878580953197200612251616917416861184388805361800660771481139764435258505831114252491932487115517415875364419"));
                  instructions.add(new VarInsnNode(25, 2));
                  // ikinci argumanin toString()'i ile karsilastir
                  instructions.add(new MethodInsnNode(182, "java/math/BigInteger", "toString", "()Ljava/lang/String;", false));
                  instructions.add(new MethodInsnNode(182, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false));
                  LabelNode label = new LabelNode();
                  // Esit degilse atla (orijinal davranis)
                  instructions.add(new JumpInsnNode(153, label)); // IFEQ
                  // Esitse: 2. argumani sahte modulus ile degistir
                  instructions.add(new TypeInsnNode(187, "java/math/BigInteger"));
                  instructions.add(new InsnNode(89)); // DUP
                  instructions.add(new LdcInsnNode("772466485993532616265602004787593990571998354762769517556824947462364733846988896764791786418284048308408035898123800088385145599914482866982121337242267111996730589833008965586904619624210600143815034555107626281306790690247380277401223658213686836260051740513369525229945146545746232288644834011082272373923840674183841152721282934896147636659685196110193170859036230613942473057350558696079781329724723538612065451271117946653171891463701206005132474249143377280956637274812017722966962038177658826682928177419967120615723412715280627140352666513761887655683881127941062521608622150057375302773817128043341551104335925307524362463158532014345920401529131689972511711145366384313357331219918007902162272947810092434401421074436297130543420857679173250879221703239461167245128718027210585470097741338476167664231386224393829173753597329684981549442031004886309176804274536524421269896699613148427017011648805968328213879561704664823249805341430850708344955922886822191380206543393525391519722222586339612770531074577303349625790208047047035933996137831086461496793235410445769659982355567180073509405077094784975713575937671560399734544873967339281900443458904052345698752817504449813508664835247206727493828740500751727305948870197"));
                  instructions.add(new MethodInsnNode(183, "java/math/BigInteger", "<init>", "(Ljava/lang/String;)V", false));
                  instructions.add(new VarInsnNode(58, 2)); // yeni degeri local 2'ye yaz
                  instructions.add(label);
                  mn.instructions.insert(instructions);
                  ClassWriter writer = new ClassWriter(3);
                  node.accept(writer);
                  return writer.toByteArray();
               }
            }

            return null;
         } catch (Exception e) {
            System.out.println(e);
            return null;
         }
      }
   }

   /** burp_patch1: Burp'un lisans dogrulama metodunun govdesini silip Filter.BurpFilter cagrisina indirge. */
   public byte[] burp_patch1(String className, byte[] classBytes) {
      if (className.startsWith("burp/") && classBytes.length > 110000) {
         ClassReader cr = new ClassReader(classBytes);
         ClassNode cn = new ClassNode();
         cr.accept(cn, 0);

         // Buyuk metot: ([Object[], Object)V ve >20000 instr
         for(MethodNode method : cn.methods) {
            if (method.desc.equals("([Ljava/lang/Object;Ljava/lang/Object;)V") && method.instructions.size() > 20000) {
               InsnList insnList = method.instructions;
               insnList.clear();                                    // Metot govdesini tamamen bosalt
               insnList.add(new VarInsnNode(25, 0));                // ALOAD 0 (arg0 = Object[])
               // Filter.BurpFilter(Object[]) static cagri
               insnList.add(new MethodInsnNode(184, "com/uyildiz/burploaderkeygen/Filter", "BurpFilter", "([Ljava/lang/Object;)V", false));
               insnList.add(new InsnNode(177));                     // RETURN
               method.exceptions.clear();                            // Exception tablosunu temizle
               method.tryCatchBlocks.clear();                        // Try-catch bloklarini temizle
            }
         }

         ClassWriter writer = new ClassWriter(3);
         cn.accept(writer);
         return writer.toByteArray();
      } else {
         return null;
      }
   }

   // ASM 9.11'in destekledigi EN YUKSEK Java major surumu (JDK 25 == 69).
   // Daha yeni (JDK 26 == 70 vb.) sinif dosyalarini ASM reddeder; bu yuzden
   // patch oncesi sinif major'unu 69'a dusurup patch sonrasi geri yukseltiyoruz.
   private static final int ASM_MAX_SUPPORTED_MAJOR = 69;

   /** Sinifin major sinif-dosyasi surumunu dondurur (byte 6-7 big-endian). */
   private static int readMajor(byte[] b) {
      return ((b[6] & 0xFF) << 8) | (b[7] & 0xFF);
   }

   /** Sinifin major surumunu yerinde yazar (byte 6-7). */
   private static void writeMajor(byte[] b, int major) {
      b[6] = (byte)((major >> 8) & 0xFF);
      b[7] = (byte)(major & 0xFF);
   }

   /** ClassFileTransformer: her sinif yuklendiginde JVM bu metodu cagirir. */
   public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classBytes) {
      // Gelen sinif ASM'nin destekleyebilecegi uzerindeyse (ornek: JDK 26 => 70)
      // ASM "Unsupported class file major version X" firlatir. Gecici olarak
      // major'u ASM'nin tanidigi en yuksek sinifa dusur, patch sonrasi geri koy.
      int origMajor = readMajor(classBytes);
      boolean downgraded = false;
      if (origMajor > ASM_MAX_SUPPORTED_MAJOR) {
         writeMajor(classBytes, ASM_MAX_SUPPORTED_MAJOR);
         downgraded = true;
      }

      byte[] result = null;
      try {
         result = this.bigint_patch(className, classBytes);
         if (result == null) result = this.bounty_patch(className, classBytes);
         if (result == null) result = this.burp_patch1(className, classBytes);
         if (result == null) result = this.burp_patch2(className, classBytes);
      } catch (Throwable t) {
         // Hicbir patch bu sinifi bozmasin: hata olursa orijinali gecir
         System.out.println("[U-YILDIZ] " + className + " icin patch atlandi: " + t.getMessage());
         result = null;
      }

      // Degistirilmediyse ve biz major'u elle dustuk ise orijinal byte'lari
      // (dogru major ile) geri dondurmemiz gerek — JVM'e yarim-degistirilmis
      // dizi gondermeyelim.
      if (downgraded) {
         writeMajor(classBytes, origMajor);                // Orijinali geri yaz
         if (result != null) writeMajor(result, origMajor); // Patch ciktisina da ayni major
      }
      return result; // null = JVM orijinali yukler
   }

   /** Java agent giris noktasi: Instrumentation'a transformer'i kayit eder. */
   public static void premain(String agentArgs, Instrumentation inst) {
      Loader loader = new Loader();        // Loader orneği
      inst.addTransformer(loader);          // Yukarida tanimli transform() cagirilir
   }
}
