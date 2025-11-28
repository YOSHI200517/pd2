import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * 販売実績管理画面
 * 日付、店舗、商品、販売数、廃棄数、返品数、返品理由、原価、売価を記録
 * AI 学習用のコアデータとなります
 */
public class SalesManagement extends JFrame {

    private static final Path DATA_FILE = Paths.get("sales.tsv");
    private final JFrame parentDashboard;

    private DefaultTableModel model;
    private JTable table;
    private TableRowSorter<javax.swing.table.TableModel> sorter;

    private JComboBox<String> storeFilter;
    private JButton addBtn, deleteBtn, editBtn, backBtn;

    public SalesManagement(JFrame dashboard) {
        this.parentDashboard = dashboard;

        setTitle("販売実績");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(buildHeader(), BorderLayout.NORTH);
        getContentPane().add(buildTableArea(), BorderLayout.CENTER);

        loadDataToModel();
        buildStoreFilterItems();
        applyDefaultSort();

        setVisible(true);
    }

    private JComponent buildHeader() {
        GradientPanel header = new GradientPanel(new Color(252, 245, 240), new Color(240, 235, 250), new BorderLayout());
        header.setBorder(new EmptyBorder(14, 16, 10, 16));

        JLabel title = new JLabel("販売実績");
        title.setFont(new Font("Yu Gothic UI", Font.BOLD, 26));
        JLabel sub = new JLabel("売上・廃棄・返品データを記録します（AI学習用）");
        sub.setForeground(new Color(110, 120, 130));
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(sub);

        storeFilter = new JComboBox<>(new String[]{"全店舗"});
        storeFilter.addActionListener(e -> applyStoreFilter());

        addBtn = new JButton("＋ 販売実績追加");
        Theme.styleButton(addBtn, new Color(100, 200, 100));
        addBtn.addActionListener(e -> openAddDialog());

        editBtn = new JButton("編集");
        Theme.styleButton(editBtn, new Color(100, 160, 220));
        editBtn.setEnabled(false);
        editBtn.addActionListener(e -> onEditSelectedRow());

        deleteBtn = new JButton("削除");
        Theme.styleButton(deleteBtn, new Color(220, 80, 80));
        deleteBtn.setEnabled(false);
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

    private JComponent buildTableArea() {
        String[] cols = {"記録日", "店舗", "商品", "販売数", "廃棄数", "返品数", "返品理由", "原価(円)", "売価(円)"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int[] sel = table.getSelectedRows();
                deleteBtn.setEnabled(sel != null && sel.length > 0);
                editBtn.setEnabled(sel != null && sel.length == 1);
            }
        });

        sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);

        // 日付は新しい順を上に
        sorter.setComparator(0, (a, b) -> {
            try {
                LocalDate da = LocalDate.parse(a.toString());
                LocalDate db = LocalDate.parse(b.toString());
                return -da.compareTo(db);
            } catch (Exception ex) {
                return 0;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(10, 16, 16, 16));

        // 廃棄数が多い行は薄赤で強調
        javax.swing.table.DefaultTableCellRenderer rowRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(table.getSelectionForeground());
                } else {
                    try {
                        int modelRow = table.convertRowIndexToModel(row);
                        Object wasteObj = table.getModel().getValueAt(modelRow, 4); // 廃棄数
                        int waste = 0;
                        if (wasteObj != null) waste = Integer.parseInt(wasteObj.toString());
                        if (waste > 0) {
                            c.setBackground(new Color(255, 240, 240));
                            c.setForeground(new Color(180, 20, 20));
                        } else {
                            c.setBackground(Color.WHITE);
                            c.setForeground(Color.BLACK);
                        }
                        if (c instanceof JComponent) ((JComponent)c).setOpaque(true);
                    } catch (Exception ex) {
                        c.setBackground(Color.WHITE);
                        c.setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rowRenderer);
        }

        return sp;
    }

    private void loadDataToModel() {
        model.setRowCount(0);
        if (!Files.exists(DATA_FILE)) return;

        new javax.swing.SwingWorker<Void, Object[]>() {
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(DATA_FILE, AppFiles.CHARSET);
                    for (String line : lines) {
                        if (line.trim().isEmpty()) continue;
                        String[] c = line.split("\t", -1);
                        if (c.length < 9) continue;
                        publish(new Object[]{c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8]});
                    }
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("loadDataToModel (sales) error", ex);
                }
                return null;
            }
            @Override protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) model.addRow(row);
            }
            @Override protected void done() {
                if (err != null) JOptionPane.showMessageDialog(SalesManagement.this,
                        "sales.tsv の読み込みに失敗: " + err, "エラー", JOptionPane.ERROR_MESSAGE);
                applyDefaultSort();
            }
        }.execute();
    }

    private void saveModelToFile() {
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

        new javax.swing.SwingWorker<Void, Void>() {
            private String err = null;
            @Override protected Void doInBackground() {
                try {
                    java.nio.file.Files.write(DATA_FILE, out, AppFiles.CHARSET);
                } catch (Exception ex) {
                    err = ex.getMessage();
                    AppLogger.error("saveModelToFile (sales) error", ex);
                }
                return null;
            }
            @Override protected void done() {
                if (err != null) {
                    JOptionPane.showMessageDialog(SalesManagement.this,
                            "sales.tsv の保存に失敗: " + err, "エラー", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void buildStoreFilterItems() {
        Set<String> shops = new TreeSet<>();
        for (int r = 0; r < model.getRowCount(); r++) {
            shops.add(model.getValueAt(r, 1).toString());
        }
        storeFilter.removeAllItems();
        storeFilter.addItem("全店舗");
        for (String s : shops) storeFilter.addItem(s);
        storeFilter.setSelectedIndex(0);
    }

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
        applyDefaultSort();
    }

    private void applyDefaultSort() {
        List<RowSorter.SortKey> keys = new ArrayList<>();
        keys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING)); // 日付の新しい順
        sorter.setSortKeys(keys);
        sorter.sort();
    }

    private void openAddDialog() {
        JTextField dateField = new JTextField(LocalDate.now().toString());
        JTextField storeField = new JTextField();
        JTextField itemField = new JTextField();

        JTextField soldField = new JTextField();    // 販売数
        JTextField wasteField = new JTextField();   // 廃棄数
        JTextField returnField = new JTextField();  // 返品数
        JTextField returnReasonField = new JTextField(); // 返品理由

        JTextField costPriceField = new JTextField();  // 原価
        JTextField sellPriceField = new JTextField();  // 売価

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("記録日"));
        form.add(dateField);
        form.add(new JLabel("店舗"));
        form.add(storeField);
        form.add(new JLabel("商品"));
        form.add(itemField);
        form.add(new JLabel("販売数"));
        form.add(soldField);
        form.add(new JLabel("廃棄数"));
        form.add(wasteField);
        form.add(new JLabel("返品数"));
        form.add(returnField);
        form.add(new JLabel("返品理由（廃棄理由）"));
        form.add(returnReasonField);
        form.add(new JLabel("原価（円）"));
        form.add(costPriceField);
        form.add(new JLabel("売価（円）"));
        form.add(sellPriceField);

        int r = JOptionPane.showConfirmDialog(this, form, "販売実績の追加",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String date = dateField.getText().trim();
        String shop = storeField.getText().trim();
        String item = itemField.getText().trim();
        String sold = soldField.getText().trim();
        String waste = wasteField.getText().trim();
        String ret = returnField.getText().trim();
        String reason = returnReasonField.getText().trim();
        String costPrice = costPriceField.getText().trim();
        String sellPrice = sellPriceField.getText().trim();

        if (date.isEmpty() || shop.isEmpty() || item.isEmpty() || sold.isEmpty() || costPrice.isEmpty() || sellPrice.isEmpty()) {
            JOptionPane.showMessageDialog(this, "必須項目が未入力です。", "入力不足", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 数値チェック
        try {
            Integer.parseInt(sold);
            Integer.parseInt(waste.isEmpty() ? "0" : waste);
            Integer.parseInt(ret.isEmpty() ? "0" : ret);
            Integer.parseInt(costPrice);
            Integer.parseInt(sellPrice);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "数値項目は整数で入力してください。");
            return;
        }

        model.addRow(new Object[]{
            date, shop, item,
            sold,
            waste.isEmpty() ? "0" : waste,
            ret.isEmpty() ? "0" : ret,
            reason,
            costPrice,
            sellPrice
        });
        saveModelToFile();
        buildStoreFilterItems();
        applyDefaultSort();
    }

    private void onDeleteSelectedRow() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows == null || viewRows.length == 0) return;

        int ans = JOptionPane.showConfirmDialog(this,
                "選択中の " + viewRows.length + " 件を削除しますか？",
                "削除の確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

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

    private void onEditSelectedRow() {
        int[] viewRows = table.getSelectedRows();
        if (viewRows == null || viewRows.length != 1) return;
        int modelRow = table.convertRowIndexToModel(viewRows[0]);

        JTextField dateField = new JTextField(String.valueOf(model.getValueAt(modelRow, 0)));
        JTextField storeField = new JTextField(String.valueOf(model.getValueAt(modelRow, 1)));
        JTextField itemField = new JTextField(String.valueOf(model.getValueAt(modelRow, 2)));
        JTextField soldField = new JTextField(String.valueOf(model.getValueAt(modelRow, 3)));
        JTextField wasteField = new JTextField(String.valueOf(model.getValueAt(modelRow, 4)));
        JTextField returnField = new JTextField(String.valueOf(model.getValueAt(modelRow, 5)));
        JTextField returnReasonField = new JTextField(String.valueOf(model.getValueAt(modelRow, 6)));
        JTextField costPriceField = new JTextField(String.valueOf(model.getValueAt(modelRow, 7)));
        JTextField sellPriceField = new JTextField(String.valueOf(model.getValueAt(modelRow, 8)));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("記録日"));
        form.add(dateField);
        form.add(new JLabel("店舗"));
        form.add(storeField);
        form.add(new JLabel("商品"));
        form.add(itemField);
        form.add(new JLabel("販売数"));
        form.add(soldField);
        form.add(new JLabel("廃棄数"));
        form.add(wasteField);
        form.add(new JLabel("返品数"));
        form.add(returnField);
        form.add(new JLabel("返品理由（廃棄理由）"));
        form.add(returnReasonField);
        form.add(new JLabel("原価（円）"));
        form.add(costPriceField);
        form.add(new JLabel("売価（円）"));
        form.add(sellPriceField);

        int r = JOptionPane.showConfirmDialog(this, form, "販売実績の編集",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String date = dateField.getText().trim();
        String shop = storeField.getText().trim();
        String item = itemField.getText().trim();
        String sold = soldField.getText().trim();
        String waste = wasteField.getText().trim();
        String ret = returnField.getText().trim();
        String reason = returnReasonField.getText().trim();
        String costPrice = costPriceField.getText().trim();
        String sellPrice = sellPriceField.getText().trim();

        if (date.isEmpty() || shop.isEmpty() || item.isEmpty() || sold.isEmpty() || costPrice.isEmpty() || sellPrice.isEmpty()) {
            JOptionPane.showMessageDialog(this, "必須項目が未入力です。", "入力不足", JOptionPane.WARNING_MESSAGE);
            return;
        }

        model.setValueAt(date, modelRow, 0);
        model.setValueAt(shop, modelRow, 1);
        model.setValueAt(item, modelRow, 2);
        model.setValueAt(sold, modelRow, 3);
        model.setValueAt(waste, modelRow, 4);
        model.setValueAt(ret, modelRow, 5);
        model.setValueAt(reason, modelRow, 6);
        model.setValueAt(costPrice, modelRow, 7);
        model.setValueAt(sellPrice, modelRow, 8);

        saveModelToFile();
        buildStoreFilterItems();
        applyDefaultSort();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SalesManagement(null).setVisible(true));
    }
}
