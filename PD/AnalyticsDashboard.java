import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * 分析ダッシュボード
 * 商品別売上×廃棄率、店舗別在庫回転日数、曜日別需要パターンなどを可視化
 */
public class AnalyticsDashboard extends JFrame {

    private final JFrame parentDashboard;
    private JLabel productWasteRateLabel, storeInventoryTurnsLabel, weeklyDemandLabel;

    public AnalyticsDashboard(JFrame dashboard) {
        this.parentDashboard = dashboard;

        setTitle("分析ダッシュボード - AI学習用");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildHeader(), BorderLayout.NORTH);
        getContentPane().add(buildAnalyticsArea(), BorderLayout.CENTER);

        setVisible(true);
    }

    private JComponent buildHeader() {
        GradientPanel header = new GradientPanel(new Color(240, 245, 255), new Color(220, 235, 250), new BorderLayout());
        header.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel title = new JLabel("分析ダッシュボード");
        title.setFont(new Font("Yu Gothic UI", Font.BOLD, 26));
        JLabel sub = new JLabel("商品別・店舗別・曜日別の分析（AI学習に使用します）");
        sub.setForeground(new Color(110, 120, 130));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        JButton backBtn = new JButton("← ダッシュボードへ戻る");
        Theme.styleButton(backBtn, new Color(100, 149, 237));
        backBtn.addActionListener(e -> {
            dispose();
            if (parentDashboard != null) parentDashboard.setVisible(true);
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(backBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JComponent buildAnalyticsArea() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBackground(new Color(250, 252, 255));
        container.setBorder(new EmptyBorder(20, 20, 20, 20));

        // === セクション1: 商品別廃棄率 ===
        container.add(createSectionTitle("📊 商品別廃棄率・粗利率"));
        productWasteRateLabel = new JLabel("データを読み込み中...");
        productWasteRateLabel.setFont(new Font("Yu Gothic UI", Font.PLAIN, 14));
        JPanel productSection = createCardSection(productWasteRateLabel);
        container.add(productSection);
        container.add(Box.createVerticalStrut(16));

        // === セクション2: 店舗別在庫回転日数 ===
        container.add(createSectionTitle("🏪 店舗別在庫回転日数"));
        storeInventoryTurnsLabel = new JLabel("データを読み込み中...");
        storeInventoryTurnsLabel.setFont(new Font("Yu Gothic UI", Font.PLAIN, 14));
        JPanel storeSection = createCardSection(storeInventoryTurnsLabel);
        container.add(storeSection);
        container.add(Box.createVerticalStrut(16));

        // === セクション3: 曜日別需要パターン ===
        container.add(createSectionTitle("📅 曜日別需要パターン"));
        weeklyDemandLabel = new JLabel("データを読み込み中...");
        weeklyDemandLabel.setFont(new Font("Yu Gothic UI", Font.PLAIN, 14));
        JPanel weeklySection = createCardSection(weeklyDemandLabel);
        container.add(weeklySection);

        container.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(container);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(new Color(250, 252, 255));

        // データ読み込み開始
        loadAnalytics();

        return scroll;
    }

    private JLabel createSectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Yu Gothic UI", Font.BOLD, 18));
        label.setForeground(new Color(50, 80, 120));
        label.setBorder(new EmptyBorder(8, 0, 8, 0));
        return label;
    }

    private JPanel createCardSection(JComponent content) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
            new LineBorder(new Color(220, 230, 240), 1),
            new EmptyBorder(16, 16, 16, 16)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private void loadAnalytics() {
        new javax.swing.SwingWorker<Map<String, String>, Void>() {
            @Override protected Map<String, String> doInBackground() {
                Map<String, String> result = new HashMap<>();
                result.put("product", analyzeProductData());
                result.put("store", analyzeStoreData());
                result.put("weekly", analyzeWeeklyData());
                return result;
            }
            @Override protected void done() {
                try {
                    Map<String, String> result = get();
                    productWasteRateLabel.setText(result.get("product"));
                    storeInventoryTurnsLabel.setText(result.get("store"));
                    weeklyDemandLabel.setText(result.get("weekly"));
                } catch (Exception ex) {
                    AppLogger.error("Analytics load error", ex);
                }
            }
        }.execute();
    }

    private String analyzeProductData() {
        Path salesFile = Paths.get("sales.tsv");
        if (!Files.exists(salesFile)) {
            return "販売データがまだ記録されていません。";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Integer> productWaste = new HashMap<>();
        Map<String, Integer> productSold = new HashMap<>();
        Map<String, Integer> productCost = new HashMap<>();
        Map<String, Integer> productPrice = new HashMap<>();

        try {
            java.util.List<String> lines = Files.readAllLines(salesFile, AppFiles.CHARSET);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split("\t", -1);
                if (cols.length < 9) continue;

                String product = cols[2];
                int sold = Integer.parseInt(cols[3]);
                int waste = Integer.parseInt(cols[4]);
                int costPrice = Integer.parseInt(cols[7]);
                int sellPrice = Integer.parseInt(cols[8]);

                productWaste.merge(product, waste, Integer::sum);
                productSold.merge(product, sold, Integer::sum);
                productCost.merge(product, costPrice, Integer::sum);
                productPrice.merge(product, sellPrice, Integer::sum);
            }

            result.append("<html>");
            for (String product : productWaste.keySet()) {
                int waste = productWaste.get(product);
                int sold = productSold.getOrDefault(product, 0);
                int total = sold + waste;
                double wasteRate = total > 0 ? (waste * 100.0 / total) : 0;
                int cost = productCost.getOrDefault(product, 0);
                int price = productPrice.getOrDefault(product, 0);
                double margin = (price - cost) > 0 ? ((price - cost) * 100.0 / price) : 0;

                result.append(String.format("• %s: 廃棄率 %.1f%% | 粗利率 %.1f%% | 販売%d個 廃棄%d個<br>",
                    product, wasteRate, margin, sold, waste));
            }
            result.append("</html>");
        } catch (Exception ex) {
            result.append("データ読み込みエラー: ").append(ex.getMessage());
        }

        return result.toString().isEmpty() ? "データなし" : result.toString();
    }

    private String analyzeStoreData() {
        Path inventoryFile = Paths.get("inventory.tsv");
        if (!Files.exists(inventoryFile)) {
            return "在庫データがまだ記録されていません。";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Integer> storeInventory = new HashMap<>();
        Map<String, Integer> storeDays = new HashMap<>();

        try {
            java.util.List<String> lines = Files.readAllLines(inventoryFile, AppFiles.CHARSET);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split("\t", -1);
                if (cols.length < 4) continue;

                String store = cols[1];
                int stock = Integer.parseInt(cols[4]);
                storeInventory.merge(store, stock, Integer::sum);
                storeDays.merge(store, 1, Integer::sum);
            }

            result.append("<html>");
            for (String store : storeInventory.keySet()) {
                int inventory = storeInventory.get(store);
                int days = storeDays.get(store);
                result.append(String.format("• %s: 在庫 %d個 | 記録日数 %d日<br>", store, inventory, days));
            }
            result.append("</html>");
        } catch (Exception ex) {
            result.append("データ読み込みエラー: ").append(ex.getMessage());
        }

        return result.toString().isEmpty() ? "データなし" : result.toString();
    }

    private String analyzeWeeklyData() {
        Path salesFile = Paths.get("sales.tsv");
        if (!Files.exists(salesFile)) {
            return "販売データがまだ記録されていません。";
        }

        StringBuilder result = new StringBuilder();
        Map<String, Integer> weeklyDemand = new HashMap<>(7);
        String[] dayNames = {"月", "火", "水", "木", "金", "土", "日"};
        for (int i = 0; i < 7; i++) {
            weeklyDemand.put(dayNames[i], 0);
        }

        try {
            java.util.List<String> lines = Files.readAllLines(salesFile, AppFiles.CHARSET);
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                String[] cols = line.split("\t", -1);
                if (cols.length < 4) continue;

                String date = cols[0];
                int sold = Integer.parseInt(cols[3]);
                try {
                    LocalDate ld = LocalDate.parse(date);
                    int dayOfWeek = ld.getDayOfWeek().getValue(); // 1=Mon, 7=Sun
                    if (dayOfWeek == 7) dayOfWeek = 0; // Sun -> last
                    String dayName = dayNames[dayOfWeek - 1];
                    weeklyDemand.merge(dayName, sold, Integer::sum);
                } catch (Exception ex) {
                    // skip
                }
            }

            result.append("<html>");
            for (String day : dayNames) {
                int demand = weeklyDemand.get(day);
                result.append(String.format("• %s曜日: %d個<br>", day, demand));
            }
            result.append("</html>");
        } catch (Exception ex) {
            result.append("データ読み込みエラー: ").append(ex.getMessage());
        }

        return result.toString().isEmpty() ? "データなし" : result.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AnalyticsDashboard(null).setVisible(true));
    }
}
