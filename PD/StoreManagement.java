import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class StoreManagement extends JFrame {

    static class Store implements Serializable {
        private static final long serialVersionUID = 1L;
        String name, category, address, contact, phone;
        Store(String n, String c, String a, String p, String ph){
            name=n; category=c; address=a; contact=p; phone=ph;
        }
    }

    private final List<Store> stores = new ArrayList<>();
    private final JPanel listPanel = new JPanel();
    private final JLabel header = new JLabel("店舗管理");
    private final JFrame parentDashboard;  // ★ 戻り先参照
    private int focusedIndex = -1;

    // ★ DashboardSimple から呼ばれるコンストラクタ
    public StoreManagement(JFrame dashboard){
        this.parentDashboard = dashboard;
        initUI();
        loadStores();
        refreshCards();
    }

    private void initUI(){
        setTitle("店舗管理");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);

        // 戻るボタン
        JButton backBtn = new JButton("← ダッシュボードへ戻る");
        backBtn.setBackground(new Color(100, 149, 237));
        backBtn.setForeground(Color.WHITE);
        backBtn.setFocusPainted(false);
        backBtn.setBorder(new EmptyBorder(10,16,10,16));
        backBtn.addActionListener(e -> {
            dispose();
            if (parentDashboard != null) {
                parentDashboard.setVisible(true);
            }
        });

        // 上部ヘッダー
        header.setFont(new Font("Yu Gothic UI", Font.BOLD, 28));
        JLabel sub = new JLabel("金沢市内のコンビニ店舗を管理します");
        sub.setForeground(new Color(90, 110, 120));

    JButton addBtn = new JButton("＋ 新規店舗");
    Theme.styleButton(addBtn, new Color(25, 160, 90));
    addBtn.addActionListener(e -> showAddDialog());
    JButton bulkDeleteBtn = new JButton("一括削除");
    Theme.styleButton(bulkDeleteBtn, new Color(200, 60, 60));
    bulkDeleteBtn.addActionListener(e -> showBulkDeleteDialog());

        JPanel topButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topButtons.setOpaque(false);
    topButtons.add(backBtn);
    topButtons.add(bulkDeleteBtn);
    topButtons.add(addBtn);

        JPanel titlePanel = new JPanel(new GridLayout(2,1));
        titlePanel.setOpaque(false);
        titlePanel.add(header); titlePanel.add(sub);

    GradientPanel top = new GradientPanel(new Color(240, 248, 245), new Color(226, 240, 250), new BorderLayout());
    top.add(titlePanel, BorderLayout.WEST);
    top.add(topButtons, BorderLayout.EAST);
    top.setBorder(new EmptyBorder(16,24,16,24));

        // リスト部分
        listPanel.setLayout(new GridLayout(0, 3, 24, 24));
        listPanel.setBorder(new EmptyBorder(24,24,24,24));
        listPanel.setBackground(new Color(245, 248, 250));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(listPanel), BorderLayout.CENTER);
    }

    // --- 保存ファイルパス ---
    private Path dataFile(){ return AppFiles.STORES_DAT; }

    // --- 保存（.dat + .tsv）★ tsvも同時更新 ---
    private void saveStores(){
        // 同期保存に変更：即時にファイルへ反映させる（保存完了前にアプリを閉じた場合の取りこぼしを防止）
        java.util.List<Store> snapshot = new ArrayList<>(stores);
        Exception err = null;
        try (ObjectOutputStream oos = new ObjectOutputStream(java.nio.file.Files.newOutputStream(dataFile()))) {
            oos.writeObject(new ArrayList<>(snapshot));
        } catch (IOException ex) {
            err = ex; AppLogger.error("saveStores error", ex);
        }
        // TSV
        java.util.List<String> lines = new ArrayList<>();
        for (Store s : snapshot) {
            String name   = tsvSafe(s.name);
            String cat    = tsvSafe(s.category);
            String addr   = tsvSafe(s.address);
            String person = tsvSafe(s.contact);
            String phone  = tsvSafe(s.phone);
            lines.add(name + "\t" + cat + "\t" + addr + "\t" + person + "\t" + phone);
        }
        try {
            java.nio.file.Files.write(AppFiles.STORES_TSV, lines, AppFiles.CHARSET,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            err = ex; AppLogger.error("writeStoresTSV error", ex);
        }

        if (err != null) {
            JOptionPane.showMessageDialog(this, "店舗情報の保存に失敗しました: " + err.getMessage(), "保存エラー", JOptionPane.ERROR_MESSAGE);
        } else {
            if (parentDashboard instanceof DashboardSimple) {
                ((DashboardSimple) parentDashboard).refreshStoreCard();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadStores(){
        Path f = dataFile();
        if (!java.nio.file.Files.exists(f)) return;
        new javax.swing.SwingWorker<Void, Store>(){
            private Exception err;
            @Override protected Void doInBackground(){
                try (ObjectInputStream ois = new ObjectInputStream(java.nio.file.Files.newInputStream(f))){
                    Object obj = ois.readObject();
                    java.util.List<Store> loaded = (ArrayList<Store>) obj;
                    for (Store s : loaded) publish(s);
                } catch(Exception ex){ err = ex; AppLogger.error("loadStores error", ex); }
                return null;
            }
            @Override protected void process(java.util.List<Store> chunks){ for (Store s : chunks) stores.add(s); }
            @Override protected void done(){ if (err != null) AppLogger.error("読み込み失敗", err); }
        }.execute();
    }

    private void showAddDialog(){
        JTextField tfName = new JTextField();
        JTextField tfCat  = new JTextField();
        JTextField tfAddr = new JTextField();
        JTextField tfPerson=new JTextField();
        JTextField tfPhone = new JTextField();

        JPanel p = new JPanel(new GridLayout(0,1,6,6));
        p.add(new JLabel("店舗名")); p.add(tfName);
        p.add(new JLabel("カテゴリ")); p.add(tfCat);
        p.add(new JLabel("住所")); p.add(tfAddr);
        p.add(new JLabel("担当者")); p.add(tfPerson);
        p.add(new JLabel("電話番号")); p.add(tfPhone);

        int r = JOptionPane.showConfirmDialog(this, p, "新規店舗の追加",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if(r==JOptionPane.OK_OPTION){
            if(tfName.getText().trim().isEmpty()){
                JOptionPane.showMessageDialog(this,"店舗名は必須です"); return;
            }
            stores.add(new Store(
                    tfName.getText().trim(),
                    tfCat.getText().trim(),
                    tfAddr.getText().trim(),
                    tfPerson.getText().trim(),
                    tfPhone.getText().trim()
            ));
            saveStores();
            refreshCards();
        }
    }

    private void refreshCards(){
        listPanel.removeAll();
        if (stores.isEmpty()) {
            listPanel.setLayout(new BorderLayout());
            JLabel msg = new JLabel("店舗がありません。［＋ 新規店舗］から登録してください。", SwingConstants.CENTER);
            msg.setForeground(new Color(120,120,130));
            listPanel.add(msg, BorderLayout.CENTER);
        } else {
            listPanel.setLayout(new GridLayout(0, 3, 24, 24));
            for (int i = 0; i < stores.size(); i++) {
                listPanel.add(storeCard(stores.get(i), i));
            }
        }
        header.setText("店舗管理（" + stores.size() + "件）");
        listPanel.revalidate();
        listPanel.repaint();

        // 一覧再描画後も保険でTSV出力（外部編集に強くするなら残す）
        writeStoresTSV(); // ★ 追加
    }

    // 一括削除ダイアログ
    private void showBulkDeleteDialog(){
        if (stores.isEmpty()){
            JOptionPane.showMessageDialog(this, "削除対象の店舗がありません。", "情報", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DefaultListModel<String> lm = new DefaultListModel<>();
        for (Store s : stores) lm.addElement(s.name + (s.category==null?"":" ("+s.category+")"));
        JList<String> list = new JList<>(lm);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane sp = new JScrollPane(list);
        sp.setPreferredSize(new Dimension(480, 360));

        int r = JOptionPane.showConfirmDialog(this, sp, "一括削除 - 削除したい店舗を選択してください",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        int[] sel = list.getSelectedIndices();
        if (sel == null || sel.length == 0) return;

        int ans = JOptionPane.showConfirmDialog(this, sel.length + " 件を削除します。よろしいですか？",
                "削除の確認", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ans != JOptionPane.YES_OPTION) return;

        java.util.Arrays.sort(sel);
        for (int i = sel.length - 1; i >= 0; i--) stores.remove(sel[i]);
        saveStores();
        refreshCards();
    }

    private JPanel storeCard(Store s, int index){
    StyledCard card = new StyledCard(Theme.STORE_TOP, Theme.STORE_BOTTOM);
    card.setBorder(new EmptyBorder(16,16,16,16));

        JLabel name = new JLabel(s.name);
        name.setFont(new Font("Yu Gothic UI", Font.BOLD, 20));

        JPanel badge = new JPanel();
        badge.setBackground(new Color(230, 240, 255));
        badge.setBorder(new EmptyBorder(4,10,4,10));
        badge.add(new JLabel(s.category==null || s.category.isEmpty()? "未分類": s.category));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(badge, BorderLayout.EAST);

        JPanel detail = new JPanel();
        detail.setOpaque(false);
        detail.setLayout(new BoxLayout(detail, BoxLayout.Y_AXIS));
        detail.add(new JLabel("📍 " + nullSafe(s.address)));
        detail.add(Box.createVerticalStrut(6));
        detail.add(new JLabel("👤 " + nullSafe(s.contact)));
        detail.add(Box.createVerticalStrut(6));
        detail.add(new JLabel("☎ " + nullSafe(s.phone)));

    JButton del = new JButton("削除");
    Theme.styleButton(del, new Color(200, 60, 60));
    del.addActionListener(e -> onDeleteByIndex(index));

    JButton edit = new JButton("編集");
    Theme.styleButton(edit, new Color(100,160,220));
    edit.addActionListener(e -> onEditByIndex(index));

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        bottom.add(edit);
        bottom.add(del);

        card.add(top, BorderLayout.NORTH);
        card.add(detail, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);
        return card;
    }

    private void onDeleteByIndex(int index){
        if (index < 0 || index >= stores.size()) return;
        Store s = stores.get(index);
        int ans = JOptionPane.showConfirmDialog(
                this,
                "「" + s.name + "」を削除しますか？",
                "削除の確認",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if(ans == JOptionPane.YES_OPTION){
            stores.remove(index);
            saveStores();
            refreshCards();
        }
    }

    private void onEditByIndex(int index) {
        if (index < 0 || index >= stores.size()) return;
        Store s = stores.get(index);
        JTextField tfName = new JTextField(s.name);
        JTextField tfCat  = new JTextField(s.category);
        JTextField tfAddr = new JTextField(s.address);
        JTextField tfPerson=new JTextField(s.contact);
        JTextField tfPhone = new JTextField(s.phone);

        JPanel p = new JPanel(new GridLayout(0,1,6,6));
        p.add(new JLabel("店舗名")); p.add(tfName);
        p.add(new JLabel("カテゴリ")); p.add(tfCat);
        p.add(new JLabel("住所")); p.add(tfAddr);
        p.add(new JLabel("担当者")); p.add(tfPerson);
        p.add(new JLabel("電話番号")); p.add(tfPhone);

        int r = JOptionPane.showConfirmDialog(this, p, "店舗情報の編集",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;
        if (tfName.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, "店舗名は必須です"); return; }

        s.name = tfName.getText().trim();
        s.category = tfCat.getText().trim();
        s.address = tfAddr.getText().trim();
        s.contact = tfPerson.getText().trim();
        s.phone = tfPhone.getText().trim();

        saveStores();
        refreshCards();
    }

    private String nullSafe(String s){ return (s==null||s.isEmpty()) ? "-" : s; }

    // ===== ここから追加：Dashboard用 TSV 出力 =====
    private void writeStoresTSV() {
        List<String> lines = new ArrayList<>();
        for (Store s : stores) {
            String name   = tsvSafe(s.name);
            String cat    = tsvSafe(s.category);
            String addr   = tsvSafe(s.address);
            String person = tsvSafe(s.contact);
            String phone  = tsvSafe(s.phone);
            lines.add(name + "\t" + cat + "\t" + addr + "\t" + person + "\t" + phone);
        }
        try {
            Files.write(AppFiles.STORES_TSV, lines, AppFiles.CHARSET,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException ex) {
            AppLogger.error("TSV保存失敗", ex);
        }
    }

    private String tsvSafe(String s){
        if (s == null) return "";
        return s.replace("\t"," ").replace("\r"," ").replace("\n"," ");
    }
    // ============================================
}
