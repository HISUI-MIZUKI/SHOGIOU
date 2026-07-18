package source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 盤面データ、お互いの山札・手札、進行ターンなど「ゲームの全状態」を管理するクラス。
 */
public class Board {
    private static Board instance;
    
    private String[][] grid; // 7x7盤面 ("P1_OU" など)
    
    // 山札 (IDの配列リスト)
    private List<String> p1Deck = new ArrayList<>();
    private List<String> p2Deck = new ArrayList<>();
    
    // 手札 (IDの配列リスト)
    private List<String> p1Hand = new ArrayList<>();
    private List<String> p2Hand = new ArrayList<>();
    
    // ゲーム進行管理
    private String currentTurn; // "P1" (先手) または "P2" (後手)
    
    // UI操作時の「選択状態」を記憶する変数
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int selectedHandIndex = -1; // 手札から召喚する場合のインデックス

    private Board() {
        grid = new String[7][7];
    }

    public static synchronized Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    /**
     * 0〜2. デッキの選択から初期手札の配布までを行うセットアップ処理
     */
    public void setupGame(String p1DeckType, String p2DeckType) {
        // 盤面クリアと王の配置
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) grid[r][c] = "";
        }
        grid[0][3] = "P2_OU"; // 後手王
        grid[6][3] = "P1_OU"; // 先手王

        // 0. マスタからデッキを取得
        p1Deck = DB_Deck.getInitialDeck(p1DeckType);
        p2Deck = DB_Deck.getInitialDeck(p2DeckType);

        // 1. 各プレイヤーの山札をシャッフル
        Collections.shuffle(p1Deck);
        Collections.shuffle(p2Deck);

        // 手札の初期化
        p1Hand.clear();
        p2Hand.clear();

        // 2. 山札の先頭から指定数引く（先手5枚、後手6枚）
        for (int i = 0; i < 5; i++) drawCard("P1");
        for (int i = 0; i < 6; i++) drawCard("P2");

        // 3. 先手（P1）の手番から開始
        currentTurn = "P1";
        clearSelection();
    }

    /**
     * 山札の先頭（インデックス0）から1枚引き、手札に加える処理
     */
    public void drawCard(String player) {
        List<String> deck = "P1".equals(player) ? p1Deck : p2Deck;
        List<String> hand = "P1".equals(player) ? p1Hand : p2Hand;
        
        if (!deck.isEmpty()) {
            String pieceId = deck.remove(0); // 先頭から消去して取得
            hand.add(pieceId);
        }
    }

    // 選択状態のリセット
    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        selectedHandIndex = -1;
    }

    // 各種ゲッター・セッター
    public String getPieceAt(int row, int col) { return grid[row][col]; }
    public void setPieceAt(int row, int col, String value) { grid[row][col] = value; }
    public List<String> getHand(String player) { return "P1".equals(player) ? p1Hand : p2Hand; }
    public List<String> getDeck(String player) { return "P1".equals(player) ? p1Deck : p2Deck; }
    public String getCurrentTurn() { return currentTurn; }
    public void switchTurn() { currentTurn = "P1".equals(currentTurn) ? "P2" : "P1"; }
    
    public int getSelectedRow() { return selectedRow; }
    public int getSelectedCol() { return selectedCol; }
    public int getSelectedHandIndex() { return selectedHandIndex; }
    public void setSelectedCell(int r, int c) { this.selectedRow = r; this.selectedCol = c; this.selectedHandIndex = -1; }
    public void setSelectedHandIndex(int index) { this.selectedHandIndex = index; this.selectedRow = -1; this.selectedCol = -1; }

    /* ====================================================================
     * 【将来の拡張ポイント】指示書に基づき、今後ここに丁寧なロジック関数を組み込みます
     * ==================================================================== */
    
    /** ③ 指定のマスに駒を置けるかチェック（未実装時は常にtrue） */
    public boolean isValidMove(int fromR, int fromC, int toR, int toC) {
        // TODO: 将棋の駒の動き、自駒の上への移動禁止、盤外、王手放置チェックなど
        return true; 
    }

    /** ③ 指定のマスに手札から召喚できるかチェック（未実装時は常にtrue） */
    public boolean isValidSpawn(String pieceId, int toR, int toC) {
        // TODO: 自陣2列以内制限、二歩、打ち歩詰めチェックなど
        return true;
    }

    /** ④ 駒を動かした後の捕獲や成り処理 */
    public void handlePostMoveProcess(int fromR, int fromC, int toR, int toC) {
        String movingPiece = grid[fromR][fromC];
        String targetPiece = grid[toR][toC];

        // 捕獲処理：相手の駒があれば、成りを戻して自分の手札に加える
        if (!targetPiece.isEmpty()) {
            String enemyPieceId = targetPiece.split("_")[1];
            // TODO: 成り駒（と金など）を元の駒に戻す処理
            getHand(currentTurn).add(enemyPieceId);
        }

        // 移動を実行
        grid[toR][toC] = movingPiece;
        grid[fromR][fromC] = "";

        // TODO: 成り判定の処理
    }

    /** ④ 手札から召喚した後の処理 */
    public void handlePostSpawnProcess(int index, int toR, int toC) {
        String pieceId = getHand(currentTurn).remove(index); // 手札から消費
        grid[toR][toC] = currentTurn + "_" + pieceId; // 盤面に配置
    }
}
