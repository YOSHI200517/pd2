import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.List;

public class DashboardSimple extends JFrame {

    // 参照ラベル
    private JLabel weatherValueLabel;
    private JLabel storeValueLabel;
    private JLabel inventoryValueLabel; // 総在庫数
    private JLabel productValueLabel; // 登録商品数

    public DashboardSimple() {
        // アプリケーションのグローバルテーマを適用（フォント等）
        Theme.applyGlobalTheme();
        setTitle("在庫管理システム - ダッシュボード");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ===== カード領域（GridBagLayout：AI分析だけ横長、縦は固定） =====
    GradientPanel cardContainer = new GradientPanel(new Color(243, 246, 250), new Color(220, 232, 245), new GridBagLayout());
    cardContainer.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(14, 14, 14, 14);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        // 1段目
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1;
        cardContainer.add(cardPanel("店舗管理", "0 店舗", new Color(171, 189, 255), false), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 1;
        cardContainer.add(cardPanel("登録商品", "0 商品", new Color(187, 235, 203), false), gbc);

        // 2段目
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
        cardContainer.add(cardPanel("在庫管理", "0 個", new Color(212, 197, 255), false), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 1;
        cardContainer.add(cardPanel("天気入力", "未入力", new Color(255, 230, 150), false), gbc);

        // 3段目（AI分析だけ横長・文字中央）…縦に伸びないようホルダーで包む
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        gbc.weighty = 1.0; // 下側の余白調整用（AIカード自体は伸ばさない）
        JPanel aiHolder = new JPanel(new BorderLayout());
        aiHolder.setOpaque(false);
        JPanel aiCard = cardPanel("AI分析", "未実装", new Color(180, 230, 255), true);
        // 高さを他カードと同等に固定（縦に伸びない）
        Dimension fixedH = new Dimension(0, 160);
        aiCard.setPreferredSize(fixedH);
        aiCard.setMinimumSize(fixedH);
        aiCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        aiHolder.add(aiCard, BorderLayout.NORTH);
        cardContainer.add(aiHolder, gbc);

    JScrollPane scroll = new JScrollPane(cardContainer);
    scroll.setBorder(null);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getVerticalScrollBar().setUnitIncrement(20);
    // ビューポート背景はグラデーションの下地に合わせた淡い色に
    scroll.getViewport().setBackground(new Color(235, 242, 250));
        add(scroll, BorderLayout.CENTER);

    // 起動時更新
    refreshWeatherCard();
    refreshStoreCard();
    refreshInventoryCard();
    refreshProductCard();

        // 戻ってきた時に再読込
        addWindowListener(new WindowAdapter() {
            @Override public void windowActivated(WindowEvent e) {
                refreshWeatherCard();
                refreshStoreCard();
                refreshInventoryCard();
                refreshProductCard();
            }
        });

        setVisible(true);
    }

    /** 共通カードUI（クリック動作含む） */
    private JPanel cardPanel(String title, String value, Color accent, boolean centerText) {
    // accent色を淡くする（白と混ぜる）
    Color top = blendColor(accent, Color.WHITE, 0.7f);
    Color bottom = blendColor(accent, Color.WHITE, 0.9f);
    StyledCard card = new StyledCard(top, bottom);
    card.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // StyledCard 自体がホバーアニメーションを持つため、ここでの設定は不要

        JLabel t = new JLabel(title);
        t.setFont(new Font("Yu Gothic UI", Font.PLAIN, 18));

        JLabel v = new JLabel(value);
        v.setFont(new Font("Yu Gothic UI", Font.BOLD, 32));

    // 参照割り当て
    if ("天気入力".equals(title))   weatherValueLabel = v;
    if ("店舗管理".equals(title))   storeValueLabel   = v;
    if ("在庫管理".equals(title))   inventoryValueLabel = v;
    if ("登録商品".equals(title))   productValueLabel = v;

        if (!centerText) {
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setOpaque(false);
            topPanel.add(t, BorderLayout.WEST);
            card.add(topPanel, BorderLayout.NORTH);
            card.add(v, BorderLayout.CENTER);
        } else {
            // 中央配置（AI分析用）
            t.setHorizontalAlignment(SwingConstants.CENTER);
            v.setHorizontalAlignment(SwingConstants.CENTER);

            card.add(Box.createVerticalStrut(8), BorderLayout.NORTH);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            t.setAlignmentX(0.5f);
            v.setAlignmentX(0.5f);
            center.add(t);
            center.add(Box.createVerticalStrut(8));
            center.add(v);
            card.add(center, BorderLayout.CENTER);
        }

        // クリック挙動（ホバーは StyledCard 側で扱う）
        card.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                switch (title) {
                    case "店舗管理":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new StoreManagement(DashboardSimple.this).setVisible(true));
                        break;
                    case "登録商品":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new ProductManagement(DashboardSimple.this).setVisible(true));
                        break;
                    case "在庫管理":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new InventoryManagement(DashboardSimple.this).setVisible(true));
                        break;
                    case "販売実績":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new SalesManagement(DashboardSimple.this).setVisible(true));
                        break;
                    case "天気入力":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new WeatherInput(DashboardSimple.this).setVisible(true));
                        break;
                    case "AI分析":
                        setVisible(false);
                        SwingUtilities.invokeLater(() -> new AnalyticsDashboard(DashboardSimple.this).setVisible(true));
                        break;
                    default:
                        JOptionPane.showMessageDialog(card, title + " が押されました（未実装）",
                                "クリック", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        // 他カードと同等の高さ
        if (!centerText) {
            Dimension d = new Dimension(0, 160);
            card.setPreferredSize(d);
            card.setMinimumSize(d);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));
        }
        return card;
    }

    /** accent色と白を混ぜて淡色を作る */
    private static Color blendColor(Color base, Color mix, float ratio) {
        int r = (int)(base.getRed() * (1 - ratio) + mix.getRed() * ratio);
        int g = (int)(base.getGreen() * (1 - ratio) + mix.getGreen() * ratio);
        int b = (int)(base.getBlue() * (1 - ratio) + mix.getBlue() * ratio);
        return new Color(r, g, b);
    }

    /** weather.tsv → 件数＋最新日付(tooltip) */
    public void refreshWeatherCard() {
        if (weatherValueLabel == null) return;
        Path p = AppFiles.WEATHER_TSV;
        if (!Files.exists(p)) {
            weatherValueLabel.setText("未入力");
            weatherValueLabel.setToolTipText(null);
            return;
        }
        // 非同期で読み込み（UI スレッド保護）
        new javax.swing.SwingWorker<Void, Void>() {
            private int count = 0;
            private String latest = null;
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p, AppFiles.CHARSET);
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        String[] cols = line.split("\t", -1);
                        if (cols.length >= 1) {
                            String date = cols[0].trim();
                            if (!date.isEmpty()) latest = (latest == null || date.compareTo(latest) > 0) ? date : latest;
                        }
                        count++;
                    }
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("refreshWeatherCard read error", ex);
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    weatherValueLabel.setText("読込エラー");
                    weatherValueLabel.setToolTipText(err);
                } else {
                    weatherValueLabel.setText(count == 0 ? "未入力" : count + " 件");
                    weatherValueLabel.setToolTipText(latest == null ? null : "最新日付: " + latest);
                }
            }
        }.execute();
    }

    /** products.tsv → 登録商品数 */
    public void refreshProductCard() {
        if (productValueLabel == null) return;
        Path p = AppFiles.PRODUCTS_TSV;
        if (!Files.exists(p)) {
            productValueLabel.setText("0 件");
            productValueLabel.setToolTipText(null);
            return;
        }
        new javax.swing.SwingWorker<Void, Void>() {
            private int count = 0;
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p, AppFiles.CHARSET);
                    for (String line : lines) if (!line.trim().isEmpty()) count++;
                } catch (Exception ex) { err = ex.getMessage(); AppLogger.error("refreshProductCard read error", ex); }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    productValueLabel.setText("読込エラー");
                    productValueLabel.setToolTipText(err);
                } else {
                    productValueLabel.setText(count + " 件");
                    productValueLabel.setToolTipText(null);
                }
            }
        }.execute();
    }

    /** stores.tsv → 店舗数 */
    public void refreshStoreCard() {
        if (storeValueLabel == null) return;
        Path p = AppFiles.STORES_TSV;
        if (!Files.exists(p)) {
            storeValueLabel.setText("0 店舗");
            return;
        }
        new javax.swing.SwingWorker<Void, Void>() {
            private int count = 0;
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p, AppFiles.CHARSET);
                    for (String line : lines) {
                        if (!line.trim().isEmpty()) count++;
                    }
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("refreshStoreCard read error", ex);
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    storeValueLabel.setText("読込エラー");
                    storeValueLabel.setToolTipText(err);
                } else {
                    storeValueLabel.setText(count + " 店舗");
                }
            }
        }.execute();
    }

    /** inventory.tsv → 在庫合計 */
    public void refreshInventoryCard() {
        if (inventoryValueLabel == null) return;
        Path p = AppFiles.INVENTORY_TSV;
        if (!Files.exists(p)) {
            inventoryValueLabel.setText("0 個");
            return;
        }
        new javax.swing.SwingWorker<Void, Void>() {
            private int total = 0;
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p, AppFiles.CHARSET);
                    // distinct 商品名の数をカウントする（例：おにぎり5個、鮭おにぎり3個 → 2）
                    java.util.Set<String> names = new java.util.HashSet<>();
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        String[] cols = line.split("\t", -1);
                        if (cols.length >= 3) {
                            String name = cols[2].trim();
                            if (!name.isEmpty()) names.add(name);
                        }
                    }
                    total = names.size();
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("refreshInventoryCard read error", ex);
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    inventoryValueLabel.setText("読込エラー");
                } else {
                    inventoryValueLabel.setText(total + " 個");
                }
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(DashboardSimple::new);
    }
}
