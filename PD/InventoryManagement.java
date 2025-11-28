import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
// import java.util.stream.Collectors; // unused

public class InventoryManagement extends JFrame {

    // TSV: 日付,店舗,商品,在庫数,発注点,状態,備考
    private static final Path DATA_FILE = AppFiles.INVENTORY_TSV;

    private final JFrame parentDashboard;

    private DefaultTableModel model;
    private JTable table;
    private TableRowSorter<TableModel> sorter;

    private JComboBox<String> storeFilter;
    private JButton addBtn;
    private JButton deleteBtn;
    private JButton editBtn;
    private JButton backBtn;

    public InventoryManagement(JFrame dashboard) {
        this.parentDashboard = dashboard;

        setTitle("在庫管理");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildHeader(), BorderLayout.NORTH);
        getContentPane().add(buildTableArea(), BorderLayout.CENTER);

        // 初期ロード
        loadDataToModel();
        buildStoreFilterItems();
        applyDefaultSort();  // 要補充が先頭に来る並び

        setVisible(true);
    }

    // ヘッダー（タイトル、フィルタ、ボタン）
    private JComponent buildHeader() {
    GradientPanel header = new GradientPanel(new Color(252, 248, 240), new Color(240, 236, 250), new BorderLayout());
    header.setBorder(new EmptyBorder(14, 16, 10, 16));

        // タイトル
        JLabel title = new JLabel("在庫管理");
        title.setFont(new Font("Yu Gothic UI", Font.BOLD, 26));
        JLabel sub   = new JLabel("各店舗の在庫状況を管理します");
        sub.setForeground(new Color(110, 120, 130));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        // 右側：フィルタとボタン
        storeFilter = new JComboBox<>(new String[]{"全店舗"});
        storeFilter.addActionListener(e -> applyStoreFilter());

    addBtn = new JButton("＋ 在庫追加");
    Theme.styleButton(addBtn, new Color(255, 165, 92));
    addBtn.addActionListener(e -> openAddDialog());

    editBtn = new JButton("編集");
    Theme.styleButton(editBtn, new Color(100, 160, 220));
    editBtn.setEnabled(false);
    editBtn.addActionListener(e -> onEditSelectedRow());

    deleteBtn = new JButton("削除");
    Theme.styleButton(deleteBtn, new Color(220, 80, 80));
    deleteBtn.setEnabled(false); // 初期は無効
    deleteBtn.addActionListener(e -> onDeleteSelectedRow());

        backBtn = new JButton("← ダッシュボードへ戻る");
        Theme.styleButton(backBtn, new Color(100, 149, 237));
        backBtn.addActionListener(e -> {
            dispose();
            if (parentDashboard != null) parentDashboard.setVisible(true);
        });

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        right.add(storeFilter);
        right.add(editBtn);
        right.add(deleteBtn);
        right.add(addBtn);
    right.add(backBtn);

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    // テーブル本体
    private JComponent buildTableArea() {
        String[] cols = {"記録日", "店舗", "商品", "賞味期限", "在庫数", "発注点", "状態", "備考"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        // 複数選択を許可
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 選択連動：選択がある場合に削除ボタンを有効化
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int[] sel = table.getSelectedRows();
                deleteBtn.setEnabled(sel != null && sel.length > 0);
                // 編集は単一選択時のみ
                editBtn.setEnabled(sel != null && sel.length == 1);
            }
        });

        // ソーター（カスタム並べ替え：要補充が上に来る）
        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        // 状態列（index=6）の比較：要補充 < 正常（=上に来る）
        sorter.setComparator(6, (a, b) -> {
            String sa = a == null ? "" : a.toString();
            String sb = b == null ? "" : b.toString();
            int ra = statusRank(sa);
            int rb = statusRank(sb);
            return Integer.compare(ra, rb);
        });
        // 日付列（index=0）は新しい方を上に
        sorter.setComparator(0, (a, b) -> {
            try {
                LocalDate da = LocalDate.parse(a.toString());
                LocalDate db = LocalDate.parse(b.toString());
                // 降順にしたいので逆
                return -da.compareTo(db);
            } catch (Exception ex) {
                return 0;
            }
        });

        // 初期のキーは applyDefaultSort() 側で設定
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(10, 16, 16, 16));
        // 行レンダラー：状態が「要補充」の場合、赤く目立たせる
        javax.swing.table.DefaultTableCellRenderer rowRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                // デフォルトの選択色を尊重
                Color selBg = table.getSelectionBackground();
                Color selFg = table.getSelectionForeground();
                try {
                    int modelRow = table.convertRowIndexToModel(row);
                    Object statusObj = table.getModel().getValueAt(modelRow, 6);
                    String status = statusObj == null ? "" : statusObj.toString().trim();
                    if (isSelected) {
                        c.setBackground(selBg);
                        c.setForeground(selFg);
                    } else if ("要補充".equals(status)) {
                        // 要補充は赤系で強調
                        c.setForeground(new Color(180, 20, 20));
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        c.setBackground(new Color(255, 240, 240));
                    } else if ("正常".equals(status)) {
                        // 正常は緑系で軽く表示
                        c.setForeground(new Color(0, 100, 40));
                        c.setBackground(new Color(240, 255, 240));
                    } else {
                        // その他は通常色
                        c.setForeground(Color.BLACK);
                        c.setBackground(Color.WHITE);
                    }
                    if (c instanceof JComponent) ((JComponent)c).setOpaque(true);
                } catch (Exception ex) {
                    // 念のためエラー発生時はデフォルトに戻す
                    c.setForeground(Color.BLACK);
                    if (!isSelected) c.setBackground(Color.WHITE);
                }
                return c;
            }
        };
        // 全カラムに適用して行単位での強調を有効化
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }
        return sp;
    }

    // テーブルの選択行を削除する（複数対応）
    private void onDeleteSelectedRow() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) return;

        // 確認メッセージに件数を表示
        int ans = JOptionPane.showConfirmDialog(this,
                "選択中の " + viewRows.length + " 件を削除しますか？",
                "削除の確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        // model側のインデックスに変換して降順で削除
        int[] modelRows = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) modelRows[i] = table.convertRowIndexToModel(viewRows[i]);
        java.util.Arrays.sort(modelRows);
        for (int i = modelRows.length - 1; i >= 0; i--) {
            model.removeRow(modelRows[i]);
        }

        saveModelToFile();
        buildStoreFilterItems();
        applyDefaultSort();
    }

    private int statusRank(String s) {
        if ("要補充".equals(s)) return 0; // 先頭
        if ("正常".equals(s)) return 1;
        return 2;
    }

    private void applyDefaultSort() {
        List<RowSorter.SortKey> keys = new ArrayList<>();
        // まず状態（要補充を先頭にするため ASC / カスタムコンパレータ）
        keys.add(new RowSorter.SortKey(5, SortOrder.ASCENDING));
        // 次に日付（新しい順を上に：Comparator側で逆転しているので ASCでOK）
        keys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(keys);
        sorter.sort();
    }

    // TSVロード
    private void loadDataToModel() {
        model.setRowCount(0);
        if (!Files.exists(DATA_FILE)) return;
        // 非同期ロード
        new javax.swing.SwingWorker<Void, Object[]>() {
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(DATA_FILE, AppFiles.CHARSET);
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        String[] c = line.split("\t", -1);
                        if (c.length < 8) continue;
                        publish(new Object[]{c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]});
                    }
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("loadDataToModel error", ex);
                }
                return null;
            }
            @Override protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) model.addRow(row);
            }
            @Override protected void done() {
                if (err != null) JOptionPane.showMessageDialog(InventoryManagement.this,
                        "inventory.tsv の読み込みに失敗: " + err, "エラー", JOptionPane.ERROR_MESSAGE);
                applyDefaultSort();
            }
        }.execute();
    }

    // TSV保存（モデル全体を書き戻し）
    private void saveModelToFile() {
        // 非同期保存（UI をブロックしない）
        java.util.List<String> out = new ArrayList<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < model.getColumnCount(); c++) {
                if (c > 0) sb.append('\t');
                Object v = model.getValueAt(r, c);
                sb.append(v == null ? "" : v.toString());
            }
            out.add(sb.toString());
        }
        new javax.swing.SwingWorker<Void, Void>(){
            private String err = null;
            @Override protected Void doInBackground(){
                try { java.nio.file.Files.write(DATA_FILE, out, AppFiles.CHARSET); }
                catch (Exception ex) { err = ex.getMessage(); AppLogger.error("saveModelToFile error", ex); }
                return null;
            }
            @Override protected void done(){
                if (err != null) {
                    JOptionPane.showMessageDialog(InventoryManagement.this,
                            "inventory.tsv の保存に失敗: " + err, "エラー", JOptionPane.ERROR_MESSAGE);
                } else {
                    // 保存成功したらダッシュボードの在庫表示を更新
                    if (parentDashboard instanceof DashboardSimple) {
                        ((DashboardSimple) parentDashboard).refreshInventoryCard();
                    }
                }
            }
        }.execute();
    }

    // フィルタの中身（店舗候補）作成
    private void buildStoreFilterItems() {
        // 既存アイテムを一旦クリアしつつ「全店舗」を先頭に
        Set<String> shops = new TreeSet<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            shops.add(model.getValueAt(r, 1).toString());
        }
        storeFilter.removeAllItems();
        storeFilter.addItem("全店舗");
        for (String s : shops) storeFilter.addItem(s);
        storeFilter.setSelectedIndex(0);
    }

    // 店舗フィルタの適用
    private void applyStoreFilter() {
        String selected = (String) storeFilter.getSelectedItem();
        if (selected == null || "全店舗".equals(selected)) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(new RowFilter<TableModel, Integer>() {
                @Override
                public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                    String shop = entry.getStringValue(1);
                    return selected.equals(shop);
                }
            });
        }
        applyDefaultSort(); // フィルタ後も並び順は維持
    }

    // 追加ダイアログ
    private void openAddDialog() {
        LocalDate today = LocalDate.now();
        JTextField dateField = new JTextField(today.toString());
        JTextField storeField = new JTextField();
        JTextField itemField  = new JTextField();

        // 賞味期限：年月日を分ける
        int thisYear = today.getYear();
        JSpinner spYear  = new JSpinner(new SpinnerNumberModel(thisYear, thisYear, thisYear + 5, 1));
        JSpinner spMonth = new JSpinner(new SpinnerNumberModel(today.getMonthValue(), 1, 12, 1));
        JSpinner spDay   = new JSpinner(new SpinnerNumberModel(today.getDayOfMonth(), 1, 31, 1));

        // 在庫数・発注点をJSpinnerで数値限定
        JSpinner stockSpinner   = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
        JSpinner reorderSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));

        JComboBox<String> statusBox = new JComboBox<>(new String[]{"正常", "要補充"});
        JTextField noteField   = new JTextField();

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("記録日"));
        form.add(dateField);
        form.add(new JLabel("店舗"));
        form.add(storeField);
        form.add(new JLabel("商品"));
        form.add(itemField);
        form.add(new JLabel("賞味期限（年）"));
        form.add(spYear);
        form.add(new JLabel("賞味期限（月）"));
        form.add(spMonth);
        form.add(new JLabel("賞味期限（日）"));
        form.add(spDay);
        form.add(new JLabel("在庫数"));
        form.add(stockSpinner);
        form.add(new JLabel("発注点"));
        form.add(reorderSpinner);
        form.add(new JLabel("状態"));
        form.add(statusBox);
        form.add(new JLabel("備考"));
        form.add(noteField);

        int r = JOptionPane.showConfirmDialog(this, form, "在庫の追加",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String date = dateField.getText().trim();
        String shop = storeField.getText().trim();
        String item = itemField.getText().trim();
        
        int year = (int) spYear.getValue();
        int month = (int) spMonth.getValue();
        int day = (int) spDay.getValue();
        String expiry = String.format("%04d-%02d-%02d", year, month, day);
        
        int stock = (int) stockSpinner.getValue();
        int reorder = (int) reorderSpinner.getValue();
        String stockStr = String.valueOf(stock);
        String reorderStr = String.valueOf(reorder);
        String status = (String) statusBox.getSelectedItem();
        String note = noteField.getText().trim();

        if (date.isEmpty() || shop.isEmpty() || item.isEmpty()) {
            JOptionPane.showMessageDialog(this, "必須項目が未入力です。", "入力不足", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // 状態の自動補正：在庫<=発注点なら要補充にする
        if (stock <= reorder) status = "要補充";

        model.addRow(new Object[]{date, shop, item, expiry, stockStr, reorderStr, status, note});
        saveModelToFile();
        buildStoreFilterItems();
        applyDefaultSort();
    }

    // 選択中の行を編集する（単一選択限定）
    private void onEditSelectedRow() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows == null || viewRows.length != 1) return;
        int modelRow = table.convertRowIndexToModel(viewRows[0]);

        String curDate = String.valueOf(model.getValueAt(modelRow, 0));
        String curShop = String.valueOf(model.getValueAt(modelRow, 1));
        String curItem = String.valueOf(model.getValueAt(modelRow, 2));
        String curExpiry = String.valueOf(model.getValueAt(modelRow, 3));
        String curStock = String.valueOf(model.getValueAt(modelRow, 4));
        String curReorder = String.valueOf(model.getValueAt(modelRow, 5));
        String curStatus = String.valueOf(model.getValueAt(modelRow, 6));
        String curNote = String.valueOf(model.getValueAt(modelRow, 7));

        JTextField dateField = new JTextField(curDate);
        JTextField storeField = new JTextField(curShop);
        JTextField itemField  = new JTextField(curItem);

        // 賞味期限を年月日に分解
        LocalDate expiryDate = LocalDate.now();
        try {
            expiryDate = LocalDate.parse(curExpiry);
        } catch (Exception ex) {
            AppLogger.warn("賞味期限パース失敗: " + ex.getMessage());
        }
        JSpinner spYear  = new JSpinner(new SpinnerNumberModel(expiryDate.getYear(), LocalDate.now().getYear(), LocalDate.now().getYear() + 5, 1));
        JSpinner spMonth = new JSpinner(new SpinnerNumberModel(expiryDate.getMonthValue(), 1, 12, 1));
        JSpinner spDay   = new JSpinner(new SpinnerNumberModel(expiryDate.getDayOfMonth(), 1, 31, 1));

        // 在庫数・発注点をJSpinnerで
        JSpinner stockSpinner   = new JSpinner(new SpinnerNumberModel(Integer.parseInt(curStock), 0, 9999, 1));
        JSpinner reorderSpinner = new JSpinner(new SpinnerNumberModel(Integer.parseInt(curReorder), 0, 9999, 1));

        JComboBox<String> statusBox = new JComboBox<>(new String[]{"正常", "要補充"});
        statusBox.setSelectedItem(curStatus);
        JTextField noteField   = new JTextField(curNote);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("記録日"));
        form.add(dateField);
        form.add(new JLabel("店舗"));
        form.add(storeField);
        form.add(new JLabel("商品"));
        form.add(itemField);
        form.add(new JLabel("賞味期限（年）"));
        form.add(spYear);
        form.add(new JLabel("賞味期限（月）"));
        form.add(spMonth);
        form.add(new JLabel("賞味期限（日）"));
        form.add(spDay);
        form.add(new JLabel("在庫数"));
        form.add(stockSpinner);
        form.add(new JLabel("発注点"));
        form.add(reorderSpinner);
        form.add(new JLabel("状態"));
        form.add(statusBox);
        form.add(new JLabel("備考"));
        form.add(noteField);

        int r = JOptionPane.showConfirmDialog(this, form, "在庫の編集",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String date = dateField.getText().trim();
        String shop = storeField.getText().trim();
        String item = itemField.getText().trim();
        
        int year = (int) spYear.getValue();
        int month = (int) spMonth.getValue();
        int day = (int) spDay.getValue();
        String expiry = String.format("%04d-%02d-%02d", year, month, day);
        
        int stock = (int) stockSpinner.getValue();
        int reorder = (int) reorderSpinner.getValue();
        String stockStr = String.valueOf(stock);
        String reorderStr = String.valueOf(reorder);
        String status = (String) statusBox.getSelectedItem();
        String note = noteField.getText().trim();

        if (date.isEmpty() || shop.isEmpty() || item.isEmpty()) {
            JOptionPane.showMessageDialog(this, "必須項目が未入力です。", "入力不足", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 状態の自動補正：在庫<=発注点なら要補充にする
        if (stock <= reorder) status = "要補充";

        model.setValueAt(date, modelRow, 0);
        model.setValueAt(shop, modelRow, 1);
        model.setValueAt(item, modelRow, 2);
        model.setValueAt(expiry, modelRow, 3);
        model.setValueAt(stockStr, modelRow, 4);
        model.setValueAt(reorderStr, modelRow, 5);
        model.setValueAt(status, modelRow, 6);
        model.setValueAt(note, modelRow, 7);

        saveModelToFile();
        buildStoreFilterItems();
        applyDefaultSort();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryManagement(null));
    }
}
