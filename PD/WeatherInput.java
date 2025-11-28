import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.*;

public class WeatherInput extends JFrame {

    private static final Path FILE = AppFiles.WEATHER_TSV; // UTF-8, タブ区切り

    private final JFrame parentDashboard;
    private final List<String[]> records = new ArrayList<>(); // {date, weather, temp, memo}
    private final DefaultListModel<String> recordModel = new DefaultListModel<>();

    public WeatherInput(JFrame dashboard) {
        this.parentDashboard = dashboard;
        setTitle("天気入力");
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // 全体レイアウト
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 249, 255));

        // ヘッダー
        JLabel title = new JLabel("天気入力");
        title.setFont(new Font("Yu Gothic UI", Font.BOLD, 26));
        JLabel sub = new JLabel("日付と天気を記録して在庫判断に活用します");
        sub.setForeground(new Color(90, 110, 120));

    JButton backBtn = new JButton("← ダッシュボードへ戻る");
    Theme.styleButton(backBtn, new Color(100, 149, 237));
        backBtn.addActionListener(e -> {
            dispose();
            if (parentDashboard != null) parentDashboard.setVisible(true);
        });

    GradientPanel header = new GradientPanel(new Color(230, 240, 255), new Color(216, 232, 250), new BorderLayout());
    JPanel titles = new JPanel(new GridLayout(2,1));
    titles.setOpaque(false);
    titles.add(title); titles.add(sub);
    header.add(titles, BorderLayout.WEST);
    header.add(backBtn, BorderLayout.EAST);
    header.setBorder(new EmptyBorder(20, 24, 20, 24));

        // 左フォーム
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(new EmptyBorder(16,16,16,16));
        formPanel.setBackground(Color.WHITE);

        JLabel dateLabel = new JLabel("日付");
        JTextField dateField = new JTextField(LocalDate.now().toString());
        JLabel weatherLabel = new JLabel("天気");
        JComboBox<String> weatherBox = new JComboBox<>(new String[]{"晴れ", "曇り", "雨", "雪"});
        JLabel tempLabel = new JLabel("気温 (℃)");
        JTextField tempField = new JTextField();
        tempField.setToolTipText("例: 15.5");
        JLabel memoLabel = new JLabel("備考");
        JTextArea memoArea = new JTextArea(3, 20);
        memoArea.setLineWrap(true);
        memoArea.setBorder(BorderFactory.createLineBorder(new Color(220,220,220)));

    JButton addBtn = new JButton("＋ 記録を追加");
    Theme.styleButton(addBtn, new Color(25, 130, 250));
        addBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        addBtn.addActionListener(e -> {
            String date = dateField.getText().trim();
            String weather = (String) weatherBox.getSelectedItem();
            String temp = tempField.getText().trim();
            String memo = memoArea.getText().trim();

            if (date.isEmpty() || temp.isEmpty()) {
                JOptionPane.showMessageDialog(this, "日付と気温を入力してください。", "入力不足", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addRecord(date, weather, temp, memo);
            memoArea.setText("");
            tempField.setText("");
        });

        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(dateLabel); formPanel.add(dateField);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(weatherLabel); formPanel.add(weatherBox);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(tempLabel); formPanel.add(tempField);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(memoLabel);
        formPanel.add(new JScrollPane(memoArea));
        formPanel.add(Box.createVerticalStrut(12));
        formPanel.add(addBtn);

    StyledCard formCard = new StyledCard(Theme.WEATHER_TOP, Theme.WEATHER_BOTTOM);
    formCard.setBorder(new CompoundBorder(
        new LineBorder(new Color(220,225,240)),
        new EmptyBorder(12,12,12,12)
    ));
    formCard.add(new JLabel("☁ 天気を記録", SwingConstants.LEFT), BorderLayout.NORTH);
    formCard.add(formPanel, BorderLayout.CENTER);

        // 右パネル（記録一覧）
        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBorder(new CompoundBorder(
                new LineBorder(new Color(220,225,240)),
                new EmptyBorder(12,12,12,12)
        ));
        JLabel listTitle = new JLabel("📅 最近の記録");
        listTitle.setFont(new Font("Yu Gothic UI", Font.BOLD, 16));
        listPanel.add(listTitle, BorderLayout.NORTH);

        JList<String> list = new JList<>(recordModel);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        list.setBackground(Color.WHITE);
        list.setFont(new Font("Yu Gothic UI", Font.PLAIN, 14));
        JScrollPane listScroll = new JScrollPane(list);

        // 削除ボタン（複数選択対応）
    JButton deleteBtn = new JButton("選択削除");
    Theme.styleButton(deleteBtn, new Color(220,80,80));
        deleteBtn.addActionListener(e -> {
            int[] sel = list.getSelectedIndices();
            if (sel == null || sel.length == 0) {
                JOptionPane.showMessageDialog(this, "削除対象を選択してください。", "情報", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            int ans = JOptionPane.showConfirmDialog(this, sel.length + " 件を削除します。よろしいですか？",
                    "削除の確認", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ans != JOptionPane.YES_OPTION) return;
            java.util.Arrays.sort(sel);
            for (int i = sel.length - 1; i >= 0; i--) {
                records.remove(sel[i]);
            }
            saveRecords();
            refreshList();
        });

        JPanel messagePanel = new JPanel(new GridBagLayout());
        JLabel emptyMsg = new JLabel("まだ記録がありません");
        emptyMsg.setForeground(new Color(130,130,140));
        messagePanel.add(emptyMsg);

        listPanel.add(messagePanel, BorderLayout.CENTER);

        recordModel.addListDataListener(new javax.swing.event.ListDataListener() {
            public void intervalAdded(javax.swing.event.ListDataEvent e) {
                listPanel.remove(messagePanel);
                listPanel.add(listScroll, BorderLayout.CENTER);
                listPanel.revalidate();
                listPanel.repaint();
            }
            public void intervalRemoved(javax.swing.event.ListDataEvent e) {}
            public void contentsChanged(javax.swing.event.ListDataEvent e) {}
        });

        // 削除ボタンは一覧の下に表示
        listPanel.add(deleteBtn, BorderLayout.SOUTH);

        // 2カラム配置
        JPanel mainContent = new JPanel(new GridLayout(1,2,20,0));
        mainContent.setBorder(new EmptyBorder(20,20,20,20));
        mainContent.setBackground(root.getBackground());
        mainContent.add(formCard);
        mainContent.add(listPanel);

        root.add(header, BorderLayout.NORTH);
        root.add(mainContent, BorderLayout.CENTER);
        add(root);

        // ★ 起動時に読み込み
        loadRecords();
        refreshList();
    }

    // ====== ここから保存・読み込みロジック ======

    private void addRecord(String date, String weather, String temp, String memo) {
        String safeMemo = memo == null || memo.isEmpty() ? "-" : memo;
        String[] row = new String[]{date, weather, temp, safeMemo};
        records.add(0, row); // 新しいものを先頭に
        recordModel.add(0, toDisplayString(row));
        saveRecords(); // 追加ごとに保存
    }

    private void refreshList() {
        recordModel.clear();
        if (records.isEmpty()) return;
        for (String[] r : records) {
            recordModel.addElement(toDisplayString(r));
        }
    }

    private String toDisplayString(String[] r) {
        return String.format("%s | %s | %s℃ | %s", r[0], r[1], r[2], r[3]);
    }

    // TSVに保存（UTF-8） — 同期保存に変更（即時ディスク書き込み）
    private void saveRecords() {
        List<String> lines = new ArrayList<>();
        for (String[] r : records) {
            lines.add(encodeTSV(r[0]) + "\t" + encodeTSV(r[1]) + "\t" + encodeTSV(r[2]) + "\t" + encodeTSV(r[3]));
        }
        java.util.List<String> snapshot = new ArrayList<>(lines);
        try {
            java.nio.file.Files.write(FILE, snapshot, AppFiles.CHARSET,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            // 保存成功したらダッシュボードを更新
            if (parentDashboard instanceof DashboardSimple) {
                ((DashboardSimple) parentDashboard).refreshWeatherCard();
            }
        } catch (Exception ex) {
            AppLogger.error("saveRecords error", ex);
            JOptionPane.showMessageDialog(WeatherInput.this, "保存に失敗しました: " + ex.getMessage(), "保存エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    // TSV読み込み（UTF-8）
    private void loadRecords() {
        records.clear();
        Path p = FILE;
        if (!Files.exists(p)) return;
        // 非同期読み込み
        new javax.swing.SwingWorker<Void, String[]>(){
            private Exception err;
            @Override protected Void doInBackground(){
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p, AppFiles.CHARSET);
                    for (String line : lines){
                        String[] cols = line.split("\t", -1);
                        if (cols.length >= 4) publish(new String[]{decodeTSV(cols[0]), decodeTSV(cols[1]), decodeTSV(cols[2]), decodeTSV(cols[3])});
                    }
                } catch (Exception ex){ err = ex; AppLogger.error("loadRecords error", ex); }
                return null;
            }
            @Override protected void process(java.util.List<String[]> chunks){ for (String[] r : chunks) records.add(r); }
            @Override protected void done(){ if (err != null) JOptionPane.showMessageDialog(WeatherInput.this, "読み込みに失敗しました: " + err.getMessage(), "読み込みエラー", JOptionPane.ERROR_MESSAGE); }
        }.execute();
    }

    // タブ・改行をエスケープ（TSV用の簡易処理）
    private String encodeTSV(String s) {
        if (s == null) return "";
        return s.replace("\t", "    ").replace("\r", " ").replace("\n", " ");
    }
    private String decodeTSV(String s) {
        return s; // 今回はencodeで潰しているのでそのまま
    }

    // ============================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new WeatherInput(null).setVisible(true));
    }
}
