import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import javax.swing.*;

/**
 * CSVエクスポート ユーティリティ
 * 在庫・販売・廃棄データを日付範囲で抽出し CSV に出力
 */
public class ExportUtil {

    /**
     * 在庫データを CSV でエクスポート
     */
    public static void exportInventoryCSV(JFrame parent, LocalDate startDate, LocalDate endDate) {
        exportTSVtoCSV(parent, Paths.get("inventory.tsv"), "inventory_export.csv", startDate, endDate);
    }

    /**
     * 販売データを CSV でエクスポート
     */
    public static void exportSalesCSV(JFrame parent, LocalDate startDate, LocalDate endDate) {
        exportTSVtoCSV(parent, Paths.get("sales.tsv"), "sales_export.csv", startDate, endDate);
    }

    /**
     * 商品データを CSV でエクスポート
     */
    public static void exportProductsCSV(JFrame parent) {
        exportTSVtoCSV(parent, AppFiles.PRODUCTS_TSV, "products_export.csv", null, null);
    }

    /**
     * 店舗データを CSV でエクスポート
     */
    public static void exportStoresCSV(JFrame parent) {
        exportTSVtoCSV(parent, AppFiles.STORES_TSV, "stores_export.csv", null, null);
    }

    /**
     * TSV を CSV に変換してエクスポート
     */
    private static void exportTSVtoCSV(JFrame parent, Path sourceFile, String exportName, LocalDate startDate, LocalDate endDate) {
        if (!Files.exists(sourceFile)) {
            JOptionPane.showMessageDialog(parent, "ソースファイルが見つかりません: " + sourceFile, "エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new javax.swing.SwingWorker<String, Void>() {
            private Exception err;

            @Override protected String doInBackground() {
                try {
                    List<String> csvLines = new ArrayList<>();
                    List<String> tsvLines = Files.readAllLines(sourceFile, AppFiles.CHARSET);

                    for (String line : tsvLines) {
                        if (line.trim().isEmpty()) continue;

                        // 日付フィルター（存在する場合）
                        if (startDate != null && endDate != null) {
                            String[] cols = line.split("\t", -1);
                            if (cols.length > 0) {
                                try {
                                    LocalDate recordDate = LocalDate.parse(cols[0]);
                                    if (recordDate.isBefore(startDate) || recordDate.isAfter(endDate)) {
                                        continue;
                                    }
                                } catch (Exception ex) {
                                    // date parse failed, include anyway
                                }
                            }
                        }

                        // TSV を CSV に変換（タブをカンマに、クォート処理）
                        String[] cols = line.split("\t", -1);
                        StringBuilder csv = new StringBuilder();
                        for (int i = 0; i < cols.length; i++) {
                            if (i > 0) csv.append(",");
                            String col = cols[i];
                            // クォート処理：カンマやダブルクォートを含む場合
                            if (col.contains(",") || col.contains("\"") || col.contains("\n")) {
                                col = "\"" + col.replace("\"", "\"\"") + "\"";
                            }
                            csv.append(col);
                        }
                        csvLines.add(csv.toString());
                    }

                    // ファイル出力
                    Path exportPath = Paths.get(exportName);
                    Files.write(exportPath, csvLines, AppFiles.CHARSET);
                    return exportName;
                } catch (Exception ex) {
                    err = ex;
                    return null;
                }
            }

            @Override protected void done() {
                if (err != null) {
                    JOptionPane.showMessageDialog(parent, "エクスポート失敗: " + err.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
                    AppLogger.error("Export failed", err);
                } else {
                    try {
                        String result = get();
                        JOptionPane.showMessageDialog(parent,
                            "✓ エクスポート完了\n\nファイル: " + result + "\n\n" +
                            "プロジェクトフォルダに保存されました。\n" +
                            "Python や R での分析に使用できます。",
                            "成功", JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        AppLogger.error("Export done error", ex);
                    }
                }
            }
        }.execute();
    }

    /**
     * ユーザーに日付範囲を入力させるダイアログ
     */
    public static LocalDate[] askDateRange(JFrame parent) {
        JTextField startField = new JTextField(LocalDate.now().minusMonths(1).toString());
        JTextField endField = new JTextField(LocalDate.now().toString());

        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("開始日 (YYYY-MM-DD):"));
        form.add(startField);
        form.add(new JLabel("終了日 (YYYY-MM-DD):"));
        form.add(endField);

        int r = JOptionPane.showConfirmDialog(parent, form, "エクスポート期間を指定",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return null;

        try {
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate end = LocalDate.parse(endField.getText().trim());
            if (start.isAfter(end)) {
                JOptionPane.showMessageDialog(parent, "開始日は終了日より前である必要があります。", "エラー", JOptionPane.ERROR_MESSAGE);
                return null;
            }
            return new LocalDate[]{start, end};
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "日付形式が不正です: " + ex.getMessage(), "エラー", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }
}
