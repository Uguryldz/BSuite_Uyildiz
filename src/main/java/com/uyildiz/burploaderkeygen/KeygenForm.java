// ============================================================================
// KeygenForm.java  —  U YILDIZ · BurpSuite Keygen Studio
// ----------------------------------------------------------------------------
// Swing tabanli modern dark-theme UI.
// Islevler:
//   1) Lisans sahibi girilince Burp lisans metnini canli uretir.
//   2) Burp'tan alinan "Activation Request" yapistirilinca otomatik olarak
//      "Activation Response" uretir.
//   3) Her alan icin "Kopyala" butonu.
//   4) Sag kolonda "Nasil Kullanilir?" rehberi.
//
// NOT: Bu sinif saf UI'dir — ag erisimi, otomatik baslatma ve ayar dosyasi
//      yoktur.
// ============================================================================
package com.uyildiz.burploaderkeygen;

import java.awt.*;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

public class KeygenForm {
   private static final String APP_NAME = "U YILDIZ · BurpSuite Keygen Studio";
   private static final String VERSION  = "v1.0";

   // ============== RENK PALETI ==============
   private static final Color BG_DARK      = new Color(18, 20, 26);
   private static final Color BG_PANEL     = new Color(28, 31, 40);
   private static final Color BG_FIELD     = new Color(38, 42, 54);
   private static final Color BG_GUIDE     = new Color(26, 30, 42);
   private static final Color BG_HEADER_1  = new Color(32, 25, 55);
   private static final Color BG_HEADER_2  = new Color(55, 35, 90);
   private static final Color FG_TEXT      = new Color(232, 234, 240);
   private static final Color FG_MUTED     = new Color(145, 150, 165);
   private static final Color ACCENT       = new Color(255, 198, 41);
   private static final Color ACCENT_HOV   = new Color(255, 215, 100);
   private static final Color ACCENT_BLUE  = new Color(78, 158, 255);
   private static final Color ACCENT_BLUE_H= new Color(120, 180, 255);
   private static final Color SUCCESS      = new Color(76, 209, 128);
   private static final Color BORDER       = new Color(60, 65, 80);
   private static final Color COPY_BG      = new Color(70, 78, 95);
   private static final Color COPY_HOV     = new Color(95, 105, 128);

   private static JLabel toast;

   // ============================================================================
   //                        UI YARDIMCI BILESENLERI
   // ============================================================================

   /** Yuvarlak koseli kenarlik. */
   private static AbstractBorder roundedBorder(final Color color, final int radius) {
      return new AbstractBorder() {
         public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
         }
         public Insets getBorderInsets(Component c) { return new Insets(4, 4, 4, 4); }
         public Insets getBorderInsets(Component c, Insets insets) { insets.set(4, 4, 4, 4); return insets; }
      };
   }

   /** Yuvarlak koseli hover efektli dugme. */
   private static JButton makeFancyButton(String text, final Color bg, final Color hoverBg) {
      final JButton b = new JButton(text) {
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = getModel().isRollover() ? hoverBg : bg;
            if (!isEnabled()) c = BORDER;
            g2.setColor(c);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
         }
      };
      b.setOpaque(false);
      b.setContentAreaFilled(false);
      b.setBorderPainted(false);
      b.setFocusPainted(false);
      b.setForeground(Color.WHITE);
      b.setFont(new Font("Segoe UI", Font.BOLD, 11));
      b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      b.setBorder(new EmptyBorder(6, 14, 6, 14));
      return b;
   }

   /** Pano'ya kopyalayan buton. */
   private static JButton makeCopyButton(final JTextComponent src, final String what) {
      JButton b = makeFancyButton("Kopyala", COPY_BG, COPY_HOV);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            try {
               String txt = src.getText() == null ? "" : src.getText();
               Toolkit.getDefaultToolkit().getSystemClipboard()
                      .setContents(new StringSelection(txt), (ClipboardOwner)null);
               showToast(what + " panoya kopyalandi");
            } catch (Exception ex) { ex.printStackTrace(); }
         }
      });
      return b;
   }

   /** Pano'dan belirtilen metin alanina icerigi yapistiran buton. */
   private static JButton makePasteButton(final JTextComponent dst, final String what) {
      JButton b = makeFancyButton("Yapistir", COPY_BG, COPY_HOV);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            try {
               Object data = Toolkit.getDefaultToolkit().getSystemClipboard()
                     .getData(java.awt.datatransfer.DataFlavor.stringFlavor);
               if (data != null) {
                  dst.setText(data.toString());
                  showToast(what + " yapistirildi");
               }
            } catch (Exception ex) { ex.printStackTrace(); }
         }
      });
      return b;
   }

   /** TextField stili. */
   private static void styleField(JTextField f) {
      f.setBackground(BG_FIELD);
      f.setForeground(FG_TEXT);
      f.setCaretColor(ACCENT);
      f.setFont(new Font("Consolas", Font.PLAIN, 13));
      f.setBorder(BorderFactory.createCompoundBorder(
         roundedBorder(BORDER, 10),
         new EmptyBorder(5, 10, 5, 10)
      ));
   }

   /** TextArea stili. */
   private static void styleArea(JTextArea a) {
      a.setBackground(BG_FIELD);
      a.setForeground(FG_TEXT);
      a.setCaretColor(ACCENT);
      a.setFont(new Font("Consolas", Font.PLAIN, 12));
      a.setLineWrap(true);
      a.setWrapStyleWord(false);
      a.setBorder(new EmptyBorder(8, 10, 8, 10));
   }

   /** TextArea'yi scroll pane icinde dondur. */
   private static JScrollPane styledScroll(JTextArea a) {
      JScrollPane sp = new JScrollPane(a,
         ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      sp.setBackground(BG_FIELD);
      sp.getViewport().setBackground(BG_FIELD);
      sp.setBorder(roundedBorder(BORDER, 10));
      return sp;
   }

   /** Baslikli panel: ust kismi ikon + baslik + sag ust aksiyonlar, govdesi scroll. */
   private static JPanel buildTitledPanel(String icon, String title, JComponent body, JComponent rightAction) {
      JPanel p = new JPanel(new BorderLayout(0, 6));
      p.setBackground(BG_PANEL);
      p.setBorder(BorderFactory.createCompoundBorder(
         roundedBorder(BORDER, 12),
         new EmptyBorder(8, 10, 10, 10)
      ));

      JPanel bar = new JPanel(new BorderLayout());
      bar.setOpaque(false);

      JLabel header = new JLabel(icon + "  " + title);
      header.setForeground(ACCENT);
      header.setFont(new Font("Segoe UI", Font.BOLD, 13));
      bar.add(header, BorderLayout.WEST);

      if (rightAction != null) bar.add(rightAction, BorderLayout.EAST);

      p.add(bar, BorderLayout.NORTH);
      p.add(body, BorderLayout.CENTER);
      return p;
   }

   /** Gradient dolgulu baslik bandi. */
   private static JPanel buildHeaderBand() {
      return new JPanel(new BorderLayout()) {
         { setOpaque(true); setBackground(BG_HEADER_1); }
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new GradientPaint(0, 0, BG_HEADER_1, getWidth(), 0, BG_HEADER_2));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(ACCENT);
            g2.fillRect(0, getHeight() - 2, getWidth(), 2);
            g2.dispose();
         }
      };
   }

   /** 5-koseli U YILDIZ amblemi. */
   private static JComponent buildStar(final int size) {
      return new JComponent() {
         { setPreferredSize(new Dimension(size, size)); }
         protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int cx = w / 2, cy = h / 2;
            int r1 = Math.min(w, h) / 2 - 2;
            int r2 = r1 / 2;
            int[] xs = new int[10], ys = new int[10];
            for (int i = 0; i < 10; i++) {
               double theta = -Math.PI / 2 + i * Math.PI / 5;
               int r = (i % 2 == 0) ? r1 : r2;
               xs[i] = (int)(cx + r * Math.cos(theta));
               ys[i] = (int)(cy + r * Math.sin(theta));
            }
            g2.setColor(ACCENT);
            g2.fillPolygon(xs, ys, 10);
            g2.setColor(ACCENT_HOV);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawPolygon(xs, ys, 10);
            g2.dispose();
         }
      };
   }

   /** Gecici bildirim. */
   private static void showToast(String msg) {
      if (toast == null) return;
      toast.setText("  ✓  " + msg + "  ");
      toast.setVisible(true);
      Timer t = new Timer(1800, new ActionListener() {
         public void actionPerformed(ActionEvent e) { toast.setVisible(false); }
      });
      t.setRepeats(false);
      t.start();
   }

   /** Nasil kullanilir rehberi (sag kolon). */
   private static JComponent buildHowToPanel() {
      JTextArea area = new JTextArea();
      area.setEditable(false);
      area.setLineWrap(true);
      area.setWrapStyleWord(true);
      area.setBackground(BG_GUIDE);
      area.setForeground(FG_TEXT);
      area.setFont(new Font("Segoe UI", Font.PLAIN, 12));
      area.setBorder(new EmptyBorder(10, 12, 10, 12));
      area.setText(
         "NASIL KULLANILIR\n" +
         "─────────────────────────────────\n\n" +
         "1) 🚀 BURP BASLAT\n" +
         "   Sag ustteki butona bas. Tek seferde:\n" +
         "     ✓ Agent JAR Burp klasorune kopyalanir\n" +
         "     ✓ BurpSuite.vmoptions'a -javaagent yazilir\n" +
         "       ve dosya SALT-OKUNUR yapilir (install4j\n" +
         "       launcher bir dahaki acilsta bozamasin).\n" +
         "     ✓ Hosts dosyasina PortSwigger phone-home\n" +
         "       engeli eklenir (yonetici izni varsa).\n" +
         "     ✓ Burp telemetri/feedback/auto-update\n" +
         "       registry ayarlari kapatilir.\n" +
         "     ✓ Burp gomulu JRE ile agent'li baslatilir.\n\n" +
         "2) LISANSI YAPISTIR\n" +
         "   • \"Lisans Sahibi\" alanina adini yaz.\n" +
         "   • [1] panelden Base64 bloku kopyala, Burp'a\n" +
         "     yapistir → Next.\n" +
         "   • Burp \"could not contact server\" diyecek →\n" +
         "     \"Manual activation\" sec.\n" +
         "   • Burp'un verdigi istegi [2]'ye Yapistir.\n" +
         "   • [3] Aktivasyon Cevabi otomatik dolar —\n" +
         "     Kopyala, Burp'a yapistir ✓\n\n" +
         "GUVENLIK / ANONIMLIK\n" +
         "─────────────────────────────────\n" +
         "KATMAN 1 — Hosts dosyasi (22 domain):\n" +
         "  portswigger.net, www.portswigger.net,\n" +
         "  portswigger-cdn.net, license/licensing/\n" +
         "  activation.portswigger.net, api/saas\n" +
         "  .licensespring.com, telemetry/analytics/\n" +
         "  crash/feedback/metrics.portswigger.net,\n" +
         "  updates/releases/update/install\n" +
         "  .portswigger.net, bappstore/store\n" +
         "  .portswigger.net, collaborator\n" +
         "  .portswigger.net, oastify.com,\n" +
         "  polling.oastify.com, browser/browser-\n" +
         "  updates.portswigger.net\n" +
         "  → DNS seviyesinde 127.0.0.1'e yonlendirir.\n" +
         "  → Burp SADECE bu domainlere ulasamaz; test\n" +
         "    ettigin HEDEF siteye normal erisir.\n\n" +
         "KATMAN 2 — Registry (telemetri kapat):\n" +
         "  HKCU\\Software\\JavaSoft\\Prefs\\burp\\...\n" +
         "   • feedbackReportingEnabled = false\n" +
         "     (crash + kullanim raporu gondermez)\n" +
         "   • AUTO_UPDATE_CHANNEL = Never\n" +
         "     (sunucudan surum sorgusu yapmaz)\n" +
         "   • collaborator.enabled = false\n" +
         "     (public Collaborator server kullanmaz)\n\n" +
         "SONUC: PortSwigger backend'ine cihaz ID,\n" +
         "installation ID, crash raporu, kullanim\n" +
         "verisi veya lisans dogrulama istegi GITMEZ.\n" +
         "Burp hala pentest hedeflerine erisebilir.\n\n" +
         "YONETICI OLARAK CALISTIRMA (TAVSIYE)\n" +
         "─────────────────────────────────\n" +
         "Hosts dosyasina yazmak icin gerekli:\n" +
         "  • UYILDIZ-Keygen.bat'a sag tik →\n" +
         "    \"Yonetici olarak calistir\"\n" +
         "Yetki yoksa hosts eklenmez — ama agent + \n" +
         "vmoptions + registry yine calisir.\n\n" +
         "LISANS TEMIZLE\n" +
         "─────────────────────────────────\n" +
         "• \"🧹 Lisans Temizle\" Burp registry'sindeki\n" +
         "  kayitli lisansi siler (HKCU\\...\\burp\\\n" +
         "  license1). Yeniden aktive etmek icin kullan."
      );
      area.setCaretPosition(0);
      JScrollPane sp = new JScrollPane(area,
         ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
         ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      sp.setBackground(BG_GUIDE);
      sp.getViewport().setBackground(BG_GUIDE);
      sp.setBorder(roundedBorder(BORDER, 10));
      return sp;
   }

   // ============================================================================
   //                                 MAIN
   // ============================================================================
   public static void main(String[] args) {
      // Nimbus L&F
      try {
         for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) { UIManager.setLookAndFeel(info.getClassName()); break; }
         }
      } catch (Exception ignored) {}

      // Lisans sahibi (opsiyonel -name arg)
      String licenseName = "U-Yildiz";
      for (int i = 0; i < args.length; i++) {
         if (("-n".equals(args[i]) || "-name".equals(args[i])) && i + 1 < args.length) {
            licenseName = args[i + 1];
         }
      }

      // === PENCERE ===
      final JFrame frame = new JFrame(APP_NAME + "  " + VERSION);
      // 'frame' zaten final (effectively) — inner class'tan erisim OK
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setSize(1180, 720);
      frame.setMinimumSize(new Dimension(1000, 620));
      frame.setLocationRelativeTo(null);

      // Root content pane — BorderLayout kullaniyoruz (null-layout YOK)
      JPanel root = new JPanel(new BorderLayout());
      root.setBackground(BG_DARK);
      frame.setContentPane(root);

      // --- UST: Baslik bandi (gradient + yildiz + marka) ---
      JPanel header = buildHeaderBand();
      header.setBorder(new EmptyBorder(10, 16, 10, 16));
      header.setPreferredSize(new Dimension(0, 72));

      JPanel brandBox = new JPanel();
      brandBox.setOpaque(false);
      brandBox.setLayout(new BoxLayout(brandBox, BoxLayout.X_AXIS));

      JComponent star = buildStar(44);
      star.setAlignmentY(Component.CENTER_ALIGNMENT);
      brandBox.add(star);
      brandBox.add(Box.createHorizontalStrut(14));

      JPanel textBox = new JPanel();
      textBox.setOpaque(false);
      textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
      JLabel brand = new JLabel("U YILDIZ");
      brand.setForeground(ACCENT);
      brand.setFont(new Font("Segoe UI", Font.BOLD, 24));
      brand.setAlignmentX(Component.LEFT_ALIGNMENT);
      JLabel sub = new JLabel("BurpSuite Keygen Studio  ·  " + VERSION);
      sub.setForeground(new Color(220, 220, 230));
      sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
      sub.setAlignmentX(Component.LEFT_ALIGNMENT);
      textBox.add(brand);
      textBox.add(Box.createVerticalStrut(2));
      textBox.add(sub);
      brandBox.add(textBox);

      header.add(brandBox, BorderLayout.WEST);

      // --- Sag tarafa 3 ana buton ---
      JPanel installBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
      installBox.setOpaque(false);

      // 🚀 Burp Baslat — tek tiklama ile hepsini halleder:
      //    agent kopyala + hosts engeli + agent'li Burp baslat
      final JButton btnRun = makeFancyButton("🚀  Burp Baslat", ACCENT, ACCENT_HOV);
      btnRun.setFont(new Font("Segoe UI", Font.BOLD, 13));
      btnRun.setForeground(new Color(30, 20, 10));
      btnRun.setToolTipText("Burp'u agent yuklu olarak baslatir. Lisans ekrani gelince keygen'den yapistirin.");
      btnRun.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            Installer.Result r = Installer.runBurpExe();
            if (r.success) {
               showToast("BurpSuite baslatiliyor...");
            } else {
               JOptionPane.showMessageDialog(frame, r.message,
                  "U YILDIZ · Burp Baslat", JOptionPane.WARNING_MESSAGE);
            }
         }
      });

      // 🧹 Lisans Temizle — registry'deki Burp lisansini sil (opsiyonel/reset)
      final JButton btnCleanLic = makeFancyButton("🧹  Lisans Temizle", new Color(130, 70, 70), new Color(170, 95, 95));
      btnCleanLic.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      btnCleanLic.setToolTipText("Burp registry'sindeki kayitli lisansi siler — sifirdan baslamak icin");
      btnCleanLic.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            int ok = JOptionPane.showConfirmDialog(frame,
               "Burp'un kayitli lisansini silmek istediginize emin misiniz?\n\n" +
               "Bu islem registry'den lisansi siler:\n" +
               "  HKCU\\Software\\JavaSoft\\Prefs\\burp\\license1\n\n" +
               "Burp bir sonraki acilista lisans ekrani gosterecek.",
               "U YILDIZ · Lisans Temizle", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
               boolean done = LicensePersister.removeLicense();
               JOptionPane.showMessageDialog(frame,
                  done ? "Burp lisansi silindi. Burp'u bir sonraki acilista yeniden aktive etmen gerek."
                       : "Lisans silinemedi (Preferences API hatasi).",
                  "U YILDIZ · Lisans Temizle",
                  done ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
               if (done) showToast("Lisans silindi");
            }
         }
      });

      installBox.add(btnRun);
      installBox.add(btnCleanLic);
      header.add(installBox, BorderLayout.EAST);

      root.add(header, BorderLayout.NORTH);

      // --- ORTA: 2 kolon (SOL = keygen, SAG = rehber) ---
      JPanel center = new JPanel(new BorderLayout(12, 0));
      center.setBackground(BG_DARK);
      center.setBorder(new EmptyBorder(14, 16, 10, 16));
      root.add(center, BorderLayout.CENTER);

      // ---- SOL KOLON: lisans sahibi + 3 panel ----
      JPanel leftCol = new JPanel(new BorderLayout(0, 10));
      leftCol.setOpaque(false);
      center.add(leftCol, BorderLayout.CENTER);

      // Lisans sahibi girdi satiri
      JPanel topInput = new JPanel(new BorderLayout(10, 0));
      topInput.setOpaque(false);
      JLabel ownerLabel = new JLabel("Lisans Sahibi:");
      ownerLabel.setForeground(FG_TEXT);
      ownerLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
      ownerLabel.setBorder(new EmptyBorder(0, 2, 0, 10));
      topInput.add(ownerLabel, BorderLayout.WEST);

      final JTextField textLicenseText = new JTextField("licensed to " + licenseName);
      styleField(textLicenseText);
      textLicenseText.setHorizontalAlignment(SwingConstants.CENTER);
      topInput.add(textLicenseText, BorderLayout.CENTER);

      JButton btnCopyOwner = makeCopyButton(textLicenseText, "Lisans sahibi");
      JPanel ownerBtnWrap = new JPanel(new BorderLayout());
      ownerBtnWrap.setOpaque(false);
      ownerBtnWrap.setBorder(new EmptyBorder(0, 10, 0, 0));
      ownerBtnWrap.add(btnCopyOwner, BorderLayout.CENTER);
      topInput.add(ownerBtnWrap, BorderLayout.EAST);

      leftCol.add(topInput, BorderLayout.NORTH);

      // Keygen panelleri icin GridBagLayout — garantili esit yerlesim
      JPanel grid = new JPanel(new GridBagLayout());
      grid.setOpaque(false);
      leftCol.add(grid, BorderLayout.CENTER);

      // [1] Lisans paneli
      final JTextArea textLicense = new JTextArea(Keygen.generateLicense(textLicenseText.getText()));
      textLicense.setEditable(false);
      styleArea(textLicense);
      JPanel panel1 = buildTitledPanel("[1]", "Lisans (License)",
                                        styledScroll(textLicense),
                                        makeCopyButton(textLicense, "Lisans"));

      // [2] Istek paneli
      final JTextArea request = new JTextArea();
      styleArea(request);
      JPanel panel2 = buildTitledPanel("[2]", "Aktivasyon Istegi",
                                        styledScroll(request),
                                        makePasteButton(request, "Istek"));

      // [3] Cevap paneli
      final JTextArea response = new JTextArea();
      response.setEditable(false);
      styleArea(response);
      JPanel panel3 = buildTitledPanel("[3]", "Aktivasyon Cevabi",
                                        styledScroll(response),
                                        makeCopyButton(response, "Cevap"));

      GridBagConstraints gc = new GridBagConstraints();
      gc.fill = GridBagConstraints.BOTH;
      gc.weightx = 0.5; gc.weighty = 1.0;
      gc.insets = new Insets(6, 0, 6, 6);

      gc.gridx = 0; gc.gridy = 0;
      grid.add(panel1, gc);

      gc.gridx = 1; gc.gridy = 0; gc.insets = new Insets(6, 6, 6, 0);
      grid.add(panel2, gc);

      gc.gridx = 0; gc.gridy = 1; gc.gridwidth = 2; gc.insets = new Insets(6, 0, 0, 0);
      grid.add(panel3, gc);

      // ---- SAG KOLON: Nasil Kullanilir rehberi ----
      JPanel rightCol = new JPanel(new BorderLayout());
      rightCol.setOpaque(false);
      rightCol.setPreferredSize(new Dimension(340, 0));

      JPanel guidePanel = new JPanel(new BorderLayout(0, 8));
      guidePanel.setBackground(BG_GUIDE);
      guidePanel.setBorder(BorderFactory.createCompoundBorder(
         roundedBorder(BORDER, 12),
         new EmptyBorder(10, 12, 12, 12)
      ));

      JLabel guideTitle = new JLabel("📘  Nasil Kullanilir?");
      guideTitle.setForeground(ACCENT);
      guideTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
      guidePanel.add(guideTitle, BorderLayout.NORTH);
      guidePanel.add(buildHowToPanel(), BorderLayout.CENTER);

      rightCol.add(guidePanel, BorderLayout.CENTER);
      center.add(rightCol, BorderLayout.EAST);

      // --- ALT: Durum cubugu + toast (JLayeredPane kullanmayalim, basit) ---
      JPanel southWrap = new JPanel(new BorderLayout());
      southWrap.setBackground(BG_PANEL);
      southWrap.setBorder(new EmptyBorder(5, 12, 5, 12));
      JLabel status = new JLabel("Hazir  —  Lisans ureteclerinize hos geldiniz.");
      status.setForeground(FG_MUTED);
      status.setFont(new Font("Segoe UI", Font.PLAIN, 11));
      southWrap.add(status, BorderLayout.WEST);

      toast = new JLabel();
      toast.setOpaque(true);
      toast.setBackground(new Color(35, 55, 45));
      toast.setForeground(SUCCESS);
      toast.setFont(new Font("Segoe UI", Font.BOLD, 11));
      toast.setBorder(BorderFactory.createCompoundBorder(
         roundedBorder(SUCCESS, 10),
         new EmptyBorder(2, 10, 2, 10)
      ));
      toast.setVisible(false);
      southWrap.add(toast, BorderLayout.EAST);
      root.add(southWrap, BorderLayout.SOUTH);

      // ---- Canli dinleyiciler ----
      textLicenseText.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) { textLicense.setText(Keygen.generateLicense(textLicenseText.getText())); }
         public void removeUpdate(DocumentEvent e) { textLicense.setText(Keygen.generateLicense(textLicenseText.getText())); }
         public void changedUpdate(DocumentEvent e) { textLicense.setText(Keygen.generateLicense(textLicenseText.getText())); }
      });

      request.getDocument().addDocumentListener(new DocumentListener() {
         public void insertUpdate(DocumentEvent e) { response.setText(Keygen.generateActivation(request.getText())); }
         public void removeUpdate(DocumentEvent e) { response.setText(Keygen.generateActivation(request.getText())); }
         public void changedUpdate(DocumentEvent e) { response.setText(Keygen.generateActivation(request.getText())); }
      });

      frame.setVisible(true);
   }
}
