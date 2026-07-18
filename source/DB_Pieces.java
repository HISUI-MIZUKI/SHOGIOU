package source;

import java.util.HashMap;
import java.util.Map;

/**
 * 将棋の各駒のマスターデータを一元定義・管理するデータベースクラス。
 * 成駒のデータも含めて登録し、画面上に正しく表示されるようにします。
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
        // 【基本の駒マスタデータ】
        registry.put("OU", new PieceData("OU", "王", "/assets/ou.png"));
        registry.put("HI", new PieceData("HI", "飛", "/assets/hi.png"));
        registry.put("KA", new PieceData("KA", "角", "/assets/ka.png"));
        registry.put("KI", new PieceData("KI", "金", "/assets/ki.png"));
        registry.put("GI", new PieceData("GI", "銀", "/assets/gi.png"));
        registry.put("KE", new PieceData("KE", "桂", "/assets/ke.png"));
        registry.put("KY", new PieceData("KY", "香", "/assets/ky.png"));
        registry.put("FU", new PieceData("FU", "歩", "/assets/fu.png"));

        // 【成駒のマスタデータ】
        registry.put("TO", new PieceData("TO", "と", "/assets/to.png"));  // と金
        registry.put("NY", new PieceData("NY", "杏", "/assets/ny.png"));  // 成香
        registry.put("NK", new PieceData("NK", "圭", "/assets/nk.png"));  // 成桂
        registry.put("NG", new PieceData("NG", "全", "/assets/ng.png"));  // 成銀
        registry.put("RY", new PieceData("RY", "龍", "/assets/ry.png"));  // 龍王
        registry.put("UM", new PieceData("UM", "馬", "/assets/um.png"));  // 龍馬
    }

    /**
     * 駒のIDからマスターデータを取得します。
     * @param key 駒ID ("OU", "HI", "TO" など)
     * @return 該当するPieceDataオブジェクト。存在しない場合はnull。
     */
    public static PieceData get(String key) {
        return registry.get(key);
    }
}
