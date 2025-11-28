import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class ProductManagement extends JFrame {

    static class Product implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, category, barcode, notes;
        int shelfDays, stock, price, costPrice; // ← 原価を追加

        Product(String n, String c, String b, int d, int s, int p, String no) {
            name = n; category = c; barcode = b; shelfDays = d; stock = s; price = p; notes = no; costPrice = 0;
        }
        Product(String n, String c, String b, int d, int s, int p, int cp, String no) {
            name = n; category = c; barcode = b; shelfDays = d; stock = s; price = p; costPrice = cp; notes = no;
        }
    }

    // カード表示はFlowLayout（折り返し）
    private final JPanel cardArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 20));
    private final DashboardSimple parentDashboard;

    private final JTextField searchField = new JTextField();
    private final JComboBox<String> categoryFilter = new JComboBox<>(new String[]{"すべて"});
    private final JLabel header = new JLabel("登録商品");
    private final JButton addBtn = new JButton("＋ 新規商品");
    private final JButton bulkDeleteBtn = new JButton("一括削除");

    private final List<Product> products = new ArrayList<>();

    private Path datFile() { return AppFiles.PRODUCTS_DAT; }

    public ProductManagement(DashboardSimple dashboard) {
        this.parentDashboard = dashboard;
        initUI();
        loadProducts();
        if (products.isEmpty()) seedDemo();
        rebuildCategoryFilter();
        renderCards();
    }

    private void initUI() {
        setTitle("登録商品");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 720);
        setLocationRelativeTo(null);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(250, 244, 252));

        // ヘッダー
        JButton backBtn = new JButton("← ダッシュボードへ戻る");
        stylePrimary(backBtn, new Color(100,149,237));
        backBtn.addActionListener(e -> { dispose(); if (parentDashboard != null) parentDashboard.setVisible(true); });

        stylePrimary(addBtn, new Color(25,160,90));
        addBtn.addActionListener(e -> showAddDialog());
    stylePrimary(bulkDeleteBtn, new Color(200,60,60));
    bulkDeleteBtn.addActionListener(e -> showBulkDeleteDialog());

        header.setFont(new Font("Yu Gothic UI", Font.BOLD, 24));
        JLabel sub = new JLabel("検索・カテゴリで絞り込み、カード形式で管理します");
        sub.setForeground(new Color(100, 110, 120));

        JPanel titleBox = new JPanel(new GridLayout(2,1));
        titleBox.setOpaque(false);
        titleBox.add(header);
        titleBox.add(sub);

        JPanel headRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        headRight.setOpaque(false);
    headRight.add(backBtn);
    headRight.add(bulkDeleteBtn);
    headRight.add(addBtn);

    GradientPanel headerBar = new GradientPanel(new Color(250, 244, 252), new Color(236, 230, 250), new BorderLayout());
    headerBar.setBorder(new EmptyBorder(10, 16, 8, 16));
    headerBar.add(titleBox, BorderLayout.WEST);
    headerBar.add(headRight, BorderLayout.EAST);

        // 検索行
        JPanel searchRow = new JPanel(new BorderLayout(10, 0));
        searchRow.setBorder(new EmptyBorder(6, 16, 6, 16));
        searchRow.setBackground(new Color(250, 244, 252));

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setBackground(Color.WHITE);
        searchWrap.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 220, 240), 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchWrap.setPreferredSize(new Dimension(0, 28));
        searchField.setBorder(null);
        searchField.setFont(new Font("Yu Gothic UI", Font.PLAIN, 14));
        searchField.setToolTipText("商品名で検索…");
        searchWrap.add(new JLabel("  🔍 "), BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        JPanel filterWrap = new JPanel(new BorderLayout());
        filterWrap.setBackground(Color.WHITE);
        filterWrap.setBorder(new CompoundBorder(
                new LineBorder(new Color(230, 220, 240), 1, true),
                new EmptyBorder(2, 10, 2, 10)
        ));
        categoryFilter.setBorder(null);
        categoryFilter.setPreferredSize(new Dimension(160, 28));
        filterWrap.add(categoryFilter, BorderLayout.CENTER);

        searchRow.add(searchWrap, BorderLayout.CENTER);
        searchRow.add(filterWrap, BorderLayout.EAST);

        // カード表示部
        cardArea.setOpaque(false);
        JScrollPane sc = new JScrollPane(cardArea);
        sc.setBorder(new EmptyBorder(12, 16, 16, 16));
        sc.getViewport().setBackground(new Color(250, 244, 252));
        sc.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sc.getVerticalScrollBar().setUnitIncrement(18);

        JPanel topArea = new JPanel(new BorderLayout());
        topArea.add(headerBar, BorderLayout.NORTH);
        topArea.add(searchRow, BorderLayout.SOUTH);
        getContentPane().add(topArea, BorderLayout.NORTH);
        getContentPane().add(sc, BorderLayout.CENTER);

        // イベント
        searchField.getDocument().addDocumentListener(new SimpleDocListener(this::renderCards));
        categoryFilter.addActionListener(e -> renderCards());
    }

    private void stylePrimary(JButton b, Color bg){
        Theme.styleButton(b, bg);
    }

    private JPanel pill(String text, Color bg){
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 4));
        p.setBackground(bg);
        p.setBorder(new EmptyBorder(2,10,2,10));
        JLabel l = new JLabel(text);
        l.setFont(new Font("Yu Gothic UI", Font.PLAIN, 12));
        p.add(l);
        return p;
    }

    /** 新規商品の追加（期限＝年/月/日の3分割入力） */
    private void showAddDialog(){
        JTextField tfName  = new JTextField();
        JTextField tfCat   = new JTextField();
        JTextField tfBarcode = new JTextField();
        // ← 期限はスピナー3つに変更
        int thisYear = LocalDate.now().getYear();
        JSpinner spYear  = new JSpinner(new SpinnerNumberModel(thisYear, thisYear, thisYear + 10, 1));
        JSpinner spMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        JSpinner spDay   = new JSpinner(new SpinnerNumberModel(LocalDate.now().getDayOfMonth(), 1, 31, 1));
        // 数値系
        JTextField tfPrice = new JTextField();
        JTextField tfCostPrice = new JTextField(); // 原価追加
        JTextField tfStock = new JTextField();
        JTextArea  taNote  = new JTextArea(3, 20);
        taNote.setLineWrap(true);
        taNote.setBorder(new LineBorder(new Color(220,220,220)));

        // 期限入力の行を横並び
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        spYear.setPreferredSize(new Dimension(80, 26));
        spMonth.setPreferredSize(new Dimension(60, 26));
        spDay.setPreferredSize(new Dimension(60, 26));
        dateRow.add(spYear);  dateRow.add(new JLabel("年"));
        dateRow.add(spMonth); dateRow.add(new JLabel("月"));
        dateRow.add(spDay);   dateRow.add(new JLabel("日"));

    JPanel form = new JPanel(new GridLayout(0,2,6,6));
    form.add(new JLabel("商品名*")); form.add(tfName);
    form.add(new JLabel("カテゴリ")); form.add(tfCat);
    form.add(new JLabel("バーコード/JAN")); form.add(tfBarcode);
    form.add(new JLabel("賞味/消費期限（日付）*")); form.add(dateRow);
    form.add(new JLabel("販売価格（円）*")); form.add(tfPrice);
    form.add(new JLabel("原価（円）")); form.add(tfCostPrice);
    form.add(new JLabel("在庫数（任意）")); form.add(tfStock);
    form.add(new JLabel("備考")); form.add(taNote);

    int r = JOptionPane.showConfirmDialog(this, form, "新規商品の追加",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (r == JOptionPane.OK_OPTION){
            String name = tfName.getText().trim();
            if (name.isEmpty()){ JOptionPane.showMessageDialog(this,"商品名は必須です"); return; }
            try {
                int year  = (Integer) spYear.getValue();
                int month = (Integer) spMonth.getValue();
                int day   = (Integer) spDay.getValue();

                // LocalDateで厳密に妥当性チェック（例: 2/30 は例外）
                LocalDate exp = LocalDate.of(year, month, day);
                LocalDate today = LocalDate.now();
                long diff = ChronoUnit.DAYS.between(today, exp);
                if (diff < 0) {
                    JOptionPane.showMessageDialog(this, "期限が過去日です。未来の日付を指定してください。");
                    return;
                }
                int shelfDays = (int) diff;

                int price = Integer.parseInt(tfPrice.getText().trim());
                int costPrice = tfCostPrice.getText().trim().isEmpty() ? 0 : Integer.parseInt(tfCostPrice.getText().trim());
                int stock = tfStock.getText().trim().isEmpty()? 0 : Integer.parseInt(tfStock.getText().trim());

                products.add(new Product(
                        name,
                        tfCat.getText().trim(),
                        tfBarcode.getText().trim(),
                        shelfDays,
                        stock,
                        price,
                        costPrice,
                        taNote.getText().trim()
                ));
                saveProducts();
                rebuildCategoryFilter();
                renderCards();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this,"価格・在庫数は整数で入力してください。");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"日付が不正です。存在する年月日を指定してください。");
            }
        }
    }

    private void renderCards(){
        String q = searchField.getText().trim().toLowerCase();
        String cat = (String) categoryFilter.getSelectedItem();

        cardArea.removeAll();

        List<Product> filtered = new ArrayList<>();
        for (Product p : products){
            boolean match = q.isEmpty() || p.name.toLowerCase().contains(q);
            boolean catOk = "すべて".equals(cat) || (p.category!=null && p.category.equals(cat));
            if (match && catOk) filtered.add(p);
        }

        if (filtered.isEmpty()){
            JLabel msg = new JLabel("該当する商品がありません。", SwingConstants.LEFT);
            msg.setForeground(new Color(130,130,140));
            cardArea.add(msg);
        } else {
            for (Product p : filtered) cardArea.add(productCard(p));
        }

        header.setText("登録商品（" + products.size() + "件）");
        cardArea.revalidate();
        cardArea.repaint();
        // TSV は保存処理側で書き出す（render 時の自動書き込みは競合の元になるため削除）
    }

    private JPanel productCard(Product p){
    StyledCard card = new StyledCard(Theme.PRODUCT_TOP, Theme.PRODUCT_BOTTOM);
    card.setBorder(new EmptyBorder(14,16,16,16));
    card.setPreferredSize(new Dimension(270, 210)); // 固定サイズで縦長防止

        JLabel name = new JLabel("<html><b>" + escape(p.name) + "</b></html>");
        name.setFont(new Font("Yu Gothic UI", Font.PLAIN, 18));

    JPanel top = new JPanel(new BorderLayout());
    top.setOpaque(false);
    top.add(name, BorderLayout.WEST);

        JPanel tag = pill((p.category==null||p.category.isEmpty())?"未分類":p.category, new Color(240,250,240));

        JPanel priceBox = new JPanel(new BorderLayout());
        priceBox.setOpaque(false);
        JLabel priceLabel = new JLabel("販売価格");
        priceLabel.setForeground(new Color(120,120,130));
        JLabel priceVal = new JLabel("¥" + p.price);
        priceVal.setFont(new Font("Yu Gothic UI", Font.BOLD, 22));
        priceBox.add(priceLabel, BorderLayout.NORTH);
        priceBox.add(Box.createVerticalStrut(4), BorderLayout.CENTER);
        priceBox.add(priceVal, BorderLayout.SOUTH);

        JLabel costLabel = new JLabel("原価: ¥" + p.costPrice);
        costLabel.setForeground(new Color(100, 100, 100));
        costLabel.setFont(new Font("Yu Gothic UI", Font.PLAIN, 12));

        JPanel meta = new JPanel();
        meta.setOpaque(false);
        meta.setLayout(new BoxLayout(meta, BoxLayout.Y_AXIS));
        meta.add(new JLabel("🔖  " + (p.barcode==null||p.barcode.isEmpty()? "-" : p.barcode)));
        meta.add(Box.createVerticalStrut(4));
        meta.add(new JLabel("⏳  期限まで: " + p.shelfDays + "日"));
        meta.add(Box.createVerticalStrut(4));
        meta.add(costLabel);

    JButton del = new JButton("削除");
    Theme.styleButton(del, new Color(200, 60, 60));
        del.addActionListener(e -> {
            int ans = JOptionPane.showConfirmDialog(this,
                    "「" + p.name + "」を削除しますか？", "削除の確認",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans == JOptionPane.YES_OPTION){
                products.remove(p);
                saveProducts();
                rebuildCategoryFilter();
                renderCards();
            }
        });

    JButton edit = new JButton("編集");
    Theme.styleButton(edit, new Color(100, 160, 220));
    edit.addActionListener(e -> onEditProduct(p));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(edit);
        bottom.add(del);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(tag, BorderLayout.NORTH);
        center.add(priceBox, BorderLayout.CENTER);

        JPanel detailWrap = new JPanel(new BorderLayout());
        detailWrap.setOpaque(false);
        detailWrap.setBorder(new CompoundBorder(
                new MatteBorder(1,0,0,0, new Color(235,235,235)),
                new EmptyBorder(8,0,0,0)
        ));
        detailWrap.add(meta, BorderLayout.CENTER);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(detailWrap, BorderLayout.SOUTH);
        card.add(bottom, BorderLayout.PAGE_END);
        return card;
    }

    private String escape(String s){ return s==null? "" : s.replace("<","&lt;").replace(">","&gt;"); }

    private void saveProducts(){
        // 同期保存（簡潔で確実）
        java.util.List<Product> snapshot = new ArrayList<>(products);
        // TSV
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (Product p : snapshot){
            lines.add(tsv(p.name) + "\t" + tsv(p.category) + "\t" +
                      tsv(p.barcode) + "\t" + p.shelfDays + "\t" +
                      p.stock + "\t" + p.price + "\t" + p.costPrice + "\t" + tsv(p.notes));
        }
        try {
            java.nio.file.Files.write(AppFiles.PRODUCTS_TSV, lines, AppFiles.CHARSET,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) { AppLogger.error("writeProductsTSV error", ex); JOptionPane.showMessageDialog(ProductManagement.this, "保存に失敗しました: " + ex.getMessage()); }

        // 保存成功後、ダッシュボードが渡されていれば件数を更新させる
        if (parentDashboard != null) parentDashboard.refreshProductCard();

        // --- 新規商品を在庫管理(inventory.tsv)にも反映 ---
        try {
            java.util.List<String> invLines = new ArrayList<>();
            java.util.Set<String> invItems = new HashSet<>();
            Path invPath = AppFiles.INVENTORY_TSV;
            if (java.nio.file.Files.exists(invPath)) {
                for (String line : java.nio.file.Files.readAllLines(invPath, AppFiles.CHARSET)) {
                    if (line.trim().isEmpty()) continue;
                    String[] c = line.split("\t", -1);
                    if (c.length >= 3) invItems.add(c[2]); // 商品名
                    invLines.add(line);
                }
            }
            LocalDate today = LocalDate.now();
            for (Product p : products) {
                if (!invItems.contains(p.name)) {
                    // 在庫数が0や空欄なら1、0より大きければその値
                    int stock = (p.stock > 0) ? p.stock : 1;
                    // 記録日, 店舗, 商品, 賞味期限, 在庫数, 発注点, 状態, 備考
                    String row = today + "\t" + "" + "\t" + p.name + "\t" + today.plusDays(p.shelfDays) + "\t" + stock + "\t0\t正常\t";
                    invLines.add(row);
                }
            }
            java.nio.file.Files.write(invPath, invLines, AppFiles.CHARSET,
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception ex) {
            AppLogger.error("auto add to inventory.tsv error", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private void loadProducts(){
        // まず TSV があればそちらを優先して読み込む（段階的移行）
        Path tsv = AppFiles.PRODUCTS_TSV;
        if (java.nio.file.Files.exists(tsv)){
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(tsv, AppFiles.CHARSET);
                for (String line : lines){
                    if (line.trim().isEmpty()) continue;  // 空行をスキップ
                    String[] c = line.split("\t", -1);
                    if (c.length >= 8){
                        // 新形式（原価あり）: name, category, barcode, shelfDays, stock, price, costPrice, notes
                        int cp = 0;
                        try { cp = Integer.parseInt(c[6]); } catch (Exception ex) {}
                        Product p = new Product(c[0], c[1], c[2], Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]), cp, c[7]);
                        products.add(p);
                    } else if (c.length >= 7) {
                        // 旧形式（原価なし）: name, category, barcode, shelfDays, stock, price, notes
                        Product p = new Product(c[0], c[1], c[2], Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]), c[6]);
                        products.add(p);
                    }
                }
            } catch (Exception ex){ 
                AppLogger.error("loadProducts from tsv error", ex);
                JOptionPane.showMessageDialog(ProductManagement.this, "商品TSVの読み込みに失敗しました: " + ex.getMessage());
            }
            return;
        }
        // TSV が無ければ従来の dat を試す
        Path f = datFile();
        if (!java.nio.file.Files.exists(f)) return;
        try (ObjectInputStream ois = new ObjectInputStream(java.nio.file.Files.newInputStream(f))){
            Object obj = ois.readObject();
            java.util.List<Product> loaded = (ArrayList<Product>) obj;
            for (Product p : loaded) products.add(p);
        } catch (java.io.EOFException ex){
            AppLogger.info("products.dat empty or truncated, skipping load");
        } catch (Exception ex){
            AppLogger.error("loadProducts error", ex);
            JOptionPane.showMessageDialog(ProductManagement.this, "商品データの読み込みに失敗しました: " + ex.getMessage());
        }
    }

    private String escapeJson(String s){ if (s==null) return ""; return s.replace("\\", "\\\\").replace("\"","\\\"").replace("\n","\\n"); }

    private void writeProductsTSV(){
        List<String> lines = new ArrayList<>();
        for (Product p : products){
            lines.add(tsv(p.name) + "\t" + tsv(p.category) + "\t" +
                      tsv(p.barcode) + "\t" + p.shelfDays + "\t" +
                      p.stock + "\t" + p.price + "\t" + tsv(p.notes));
        }
        try {
            Files.write(AppFiles.PRODUCTS_TSV, lines, AppFiles.CHARSET,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            AppLogger.error("TSV保存失敗", ex);
        }
    }

    private String tsv(String s){ return (s==null)?"":s.replace("\t"," ").replace("\r"," ").replace("\n"," "); }

    private void rebuildCategoryFilter(){
        Object sel = categoryFilter.getSelectedItem();
        Set<String> cats = new TreeSet<>();
        for (Product p : products) if (p.category!=null && !p.category.isEmpty()) cats.add(p.category);
        categoryFilter.removeAllItems();
        categoryFilter.addItem("すべて");
        for (String c : cats) categoryFilter.addItem(c);
        if (sel != null) categoryFilter.setSelectedItem(sel);
    }

    private void seedDemo(){
        products.add(new Product("鮭おにぎり","おにぎり","4901234567891",2,0,120,80,""));
        products.add(new Product("幕の内弁当","弁当","4901234567890",1,0,580,300,""));
        products.add(new Product("メロンパン","パン","4901234567892",3,0,150,70,""));
        saveProducts();
    }

    // 商品の編集ダイアログ（カード上の編集ボタンから呼ばれる）
    private void onEditProduct(Product p) {
        if (p == null) return;
        // プリセットして追加ダイアログと同様のフォームを表示
        JTextField tfName  = new JTextField(p.name);
        JTextField tfCat   = new JTextField(p.category);
        JTextField tfBarcode = new JTextField(p.barcode);

        int thisYear = LocalDate.now().getYear();
        // 賞味期限は残日数から復元は難しいため、既存の残日数を年数に直さずそのまま表示させる簡易実装
        JSpinner spYear  = new JSpinner(new SpinnerNumberModel(thisYear, thisYear, thisYear + 10, 1));
        JSpinner spMonth = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        JSpinner spDay   = new JSpinner(new SpinnerNumberModel(LocalDate.now().getDayOfMonth(), 1, 31, 1));

        JTextField tfPrice = new JTextField(String.valueOf(p.price));
        JTextField tfCostPrice = new JTextField(String.valueOf(p.costPrice));
        JTextField tfStock = new JTextField(String.valueOf(p.stock));
        JTextArea  taNote  = new JTextArea(p.notes == null ? "" : p.notes, 3, 20);
        taNote.setLineWrap(true);
        taNote.setBorder(new LineBorder(new Color(220,220,220)));

        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        spYear.setPreferredSize(new Dimension(80, 26));
        spMonth.setPreferredSize(new Dimension(60, 26));
        spDay.setPreferredSize(new Dimension(60, 26));
        dateRow.add(spYear);  dateRow.add(new JLabel("年"));
        dateRow.add(spMonth); dateRow.add(new JLabel("月"));
        dateRow.add(spDay);   dateRow.add(new JLabel("日"));

        JPanel form = new JPanel(new GridLayout(0,1,6,6));
        form.add(new JLabel("商品名*")); form.add(tfName);
        form.add(new JLabel("カテゴリ")); form.add(tfCat);
        form.add(new JLabel("バーコード/JAN")); form.add(tfBarcode);
        form.add(new JLabel("賞味/消費期限（日付）*")); form.add(dateRow);
        form.add(new JLabel("販売価格（円）*")); form.add(tfPrice);
        form.add(new JLabel("原価（円）")); form.add(tfCostPrice);
        form.add(new JLabel("在庫数（任意）")); form.add(tfStock);
        form.add(new JLabel("備考")); form.add(new JScrollPane(taNote));

        JScrollPane sp = new JScrollPane(form);
        sp.setPreferredSize(new Dimension(440, 380));

        int r = JOptionPane.showConfirmDialog(this, sp, "商品の編集",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r == JOptionPane.OK_OPTION){
            String name = tfName.getText().trim();
            if (name.isEmpty()){ JOptionPane.showMessageDialog(this,"商品名は必須です"); return; }
            try {
                // 日付は簡易に検証のみ
                int price = Integer.parseInt(tfPrice.getText().trim());
                int costPrice = tfCostPrice.getText().trim().isEmpty() ? 0 : Integer.parseInt(tfCostPrice.getText().trim());
                int stock = tfStock.getText().trim().isEmpty()? 0 : Integer.parseInt(tfStock.getText().trim());

                // 更新
                p.name = name;
                p.category = tfCat.getText().trim();
                p.barcode = tfBarcode.getText().trim();
                p.price = price;
                p.costPrice = costPrice;
                p.stock = stock;
                p.notes = taNote.getText().trim();

                saveProducts();
                rebuildCategoryFilter();
                renderCards();
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this,"価格・在庫数は整数で入力してください。");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,"入力エラー");
            }
        }
    }

    // 一括削除ダイアログ
    private void showBulkDeleteDialog(){
        if (products.isEmpty()){
            JOptionPane.showMessageDialog(this, "削除対象の商品がありません。", "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultListModel<String> lm = new DefaultListModel<>();
        for (Product p : products) lm.addElement(p.name + (p.category==null?"":" ("+p.category+")"));
        JList<String> list = new JList<>(lm);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(420, 360));

        int r = JOptionPane.showConfirmDialog(this, sp, "一括削除 - 削除したい商品を選択してください",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        int[] sel = list.getSelectedIndices();
        if (sel == null || sel.length == 0) return;

        // 確認
        int ans = JOptionPane.showConfirmDialog(this, sel.length + " 件を削除します。よろしいですか？",
                "削除の確認", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        // 削除は降順で
        java.util.Arrays.sort(sel);
        for (int i = sel.length - 1; i >= 0; i--) products.remove(sel[i]);
        saveProducts();
        rebuildCategoryFilter();
        renderCards();
    }

    private static class SimpleDocListener implements javax.swing.event.DocumentListener {
        private final Runnable fn;
        SimpleDocListener(Runnable r){ fn=r; }
        public void insertUpdate(javax.swing.event.DocumentEvent e){ fn.run(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e){ fn.run(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e){ fn.run(); }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ProductManagement(null).setVisible(true));
    }
}
