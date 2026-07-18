package source;

import java.util.HashMap;
import java.util.Map;

/**
 * 将棋の各駒のマスターデータを一元定義・管理するデータベースクラス。
 * 駒のID、画像がない場合の代替漢字、画像ファイルへのパスを紐づけます。
 */
public class DB_Pieces {
    
    /**
     * 各駒の個別データを保持する構造体クラス。
     */
    public static class PieceData {
        public String id;          // 識別キー (例: "OU", "HI")
        public String displayName; // 画像がないときに代わりに表示する漢字1文字 (例: "王", "飛")
        public String imagePath;   // グラフィック画像ファイルへのプロジェクト内リソースパス

        public PieceData(String id, String displayName, String imagePath) {
            this.id = id;
            this.displayName = displayName;
            this.imagePath = imagePath;
        }
    }

    // 全ての駒データをIDをキーにして保持する連想配列
    private static final Map<String, PieceData> registry = new HashMap<>();

    static {
        // 【マスタデータ登録】
        // 今後画像アセットを追加する場合は、第3引数のパスに実際のファイルを配置します。
        // ファイルが見つからない間は、自動的に第2引数の漢字が盤面に適用されます。
        registry.put("OU", new PieceData("OU", "王", "/assets/ou.png"));
        registry.put("HI", new PieceData("HI", "飛", "/assets/hi.png"));
        registry.put("KA", new PieceData("KA", "角", "/assets/ka.png"));
        registry.put("KI", new PieceData("KI", "金", "/assets/ki.png"));
        registry.put("GI", new PieceData("GI", "銀", "/assets/gi.png"));
        registry.put("KE", new PieceData("KE", "桂", "/assets/ke.png"));
        registry.put("KY", new PieceData("KY", "香", "/assets/ky.png"));
        registry.put("FU", new PieceData("FU", "歩", "/assets/fu.png"));
    }

    /**
     * 駒のIDからマスターデータを取得します。
     * @param key 駒ID ("OU", "HI" など)
     * @return 該当するPieceDataオブジェクト。存在しない場合はnull。
     */
    public static PieceData get(String key) {
        return registry.get(key);
    }
}
