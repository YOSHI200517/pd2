import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * アプリ内で使うファイルパスと文字セットを集中管理する定数クラス。
 * 低リスクなリファクタで、ハードコードされたファイル名を一箇所にまとめます。
 */
public final class AppFiles {
    public static final java.nio.charset.Charset CHARSET = java.nio.charset.StandardCharsets.UTF_8;

    public static final Path INVENTORY_TSV = Paths.get("inventory.tsv");
    public static final Path PRODUCTS_TSV  = Paths.get("products.tsv");
    public static final Path PRODUCTS_DAT  = Paths.get("products.dat");
    public static final Path PRODUCTS_JSON = Paths.get("products.json");
    public static final Path STORES_TSV    = Paths.get("stores.tsv");
    public static final Path STORES_DAT    = Paths.get("stores.dat");
    public static final Path WEATHER_TSV   = Paths.get("weather.tsv");

    private AppFiles(){}
}
