import java.awt.Color;

// ボタンスタイル用のユーティリティは Theme クラス内の static メソッドとして提供します。


/**
 * アプリケーションのテーマ色を一元管理します。
 */
public final class Theme {
    // ダッシュボード
    public static final Color DASHBOARD_TOP = new Color(250, 253, 255);
    public static final Color DASHBOARD_BOTTOM = new Color(234, 245, 254);

    // 在庫画面
    public static final Color INVENTORY_TOP = new Color(252, 248, 240);
    public static final Color INVENTORY_BOTTOM = new Color(240, 236, 250);

    // 商品画面
    public static final Color PRODUCT_TOP = new Color(250, 244, 252);
    public static final Color PRODUCT_BOTTOM = new Color(236, 230, 250);

    // 店舗画面
    public static final Color STORE_TOP = new Color(240, 248, 245);
    public static final Color STORE_BOTTOM = new Color(226, 240, 250);

    // 天気画面
    public static final Color WEATHER_TOP = new Color(230, 240, 255);
    public static final Color WEATHER_BOTTOM = new Color(216, 232, 250);

    // カード影
    public static final Color CARD_SHADOW = new Color(0, 0, 0, 36);

    // グローバルフォント
    public static final java.awt.Font APP_FONT = new java.awt.Font("Yu Gothic UI", java.awt.Font.PLAIN, 13);
    public static final java.awt.Font APP_FONT_BOLD = APP_FONT.deriveFont(java.awt.Font.BOLD);

    // ボタンにホバー効果を付与（外部で呼び出して使う）
    public static void styleButton(javax.swing.JButton b, Color bg) {
        if (b == null || bg == null) return;
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new javax.swing.border.EmptyBorder(8, 14, 8, 14));
        b.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        Color darker = bg.darker();
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(darker); }
            @Override public void mouseExited(java.awt.event.MouseEvent e)  { b.setBackground(bg); }
        });
    }

    /**
     * アプリ全体向けの UI デフォルトを適用します。起動直後に一度呼んでください。
     */
    public static void applyGlobalTheme() {
        try {
            javax.swing.UIManager.put("Label.font", APP_FONT);
            javax.swing.UIManager.put("Button.font", APP_FONT);
            javax.swing.UIManager.put("TextField.font", APP_FONT);
            javax.swing.UIManager.put("TextArea.font", APP_FONT);
            javax.swing.UIManager.put("ComboBox.font", APP_FONT);
            javax.swing.UIManager.put("Table.font", APP_FONT);
            javax.swing.UIManager.put("TableHeader.font", APP_FONT_BOLD);
            javax.swing.UIManager.put("ToolTip.font", APP_FONT);
            // ToolTip 見た目の微調整
            javax.swing.UIManager.put("ToolTip.background", new java.awt.Color(255, 255, 240));
            javax.swing.UIManager.put("ToolTip.border", new javax.swing.border.LineBorder(new java.awt.Color(200,200,200)));
        } catch (Exception ex) {
            // 無害な失敗はログに残す
            AppLogger.error("applyGlobalTheme failed", ex);
        }
    }

    private Theme() {}
}
