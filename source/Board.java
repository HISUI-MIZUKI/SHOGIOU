package source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 盤面データ、お互いの山札・手札、進行ターンなど「ゲームの全状態」を管理するクラス。
 * 仮想空間（バーチャル盤面）を用いた安全な王手・詰み・禁止手チェック関数を実装。
 */
public class Board {
    private static Board instance;
    
    private String[][] grid; // 7x7盤面 ("P1_OU", "P2_FU" など)
    
    // 山札・手札 (IDの配列リスト)
    private List<String> p1Deck = new ArrayList<>();
    private List<String> p2Deck = new ArrayList<>();
    private List<String> p1Hand = new ArrayList<>();
    private List<String> p2Hand = new ArrayList<>();
    
    // ゲーム進行管理
    private String currentTurn; // "P1" (先手) または "P2" (後手)
    
    // UI操作時の「選択状態」を記憶する変数
    private int selectedRow = -1;
    private int selectedCol = -1;
    private int selectedHandIndex = -1;

    private Board() {
        grid = new String[7][7];
    }

    public static synchronized Board getInstance() {
        if (instance == null) {
            instance = new Board();
        }
        return instance;
    }

    public void setupGame(String p1DeckType, String p2DeckType) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) grid[r][c] = "";
        }
        grid[0][3] = "P2_OU"; // 後手王
        grid[6][3] = "P1_OU"; // 先手王

        p1Deck = DB_Deck.getInitialDeck(p1DeckType);
        p2Deck = DB_Deck.getInitialDeck(p2DeckType);

        Collections.shuffle(p1Deck);
        Collections.shuffle(p2Deck);

        p1Hand.clear();
        p2Hand.clear();

        for (int i = 0; i < 5; i++) drawCard("P1");
        for (int i = 0; i < 6; i++) drawCard("P2");

        currentTurn = "P1";
        clearSelection();
    }

    public void drawCard(String player) {
        List<String> deck = "P1".equals(player) ? p1Deck : p2Deck;
        List<String> hand = "P1".equals(player) ? p1Hand : p2Hand;
        if (!deck.isEmpty()) {
            hand.add(deck.remove(0));
        }
    }

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
     *  コア・ロジック：移動および召喚のバリデーション (バーチャル空間を利用)
     * ==================================================================== */
    
    /**
     * 指定のマスに駒を移動できるかチェックするメイン関数
     */
    public boolean isValidMove(int fromR, int fromC, int toR, int toC) {
        // 盤外チェック
        if (fromR < 0 || fromR >= 7 || fromC < 0 || fromC >= 7 || toR < 0 || toR >= 7 || toC < 0 || toC >= 7) return false;
        
        String piece = grid[fromR][fromC];
        if (piece.isEmpty()) return false;
        
        String player = piece.substring(0, 2); // "P1" または "P2"
        String target = grid[toR][toC];
        
        // 移動先に自分の駒がある場合は移動不可
        if (!target.isEmpty() && target.startsWith(player)) return false;

        // 1. 駒固有の動きとして正しいかチェック
        if (!isValidPieceTrajectory(grid, fromR, fromC, toR, toC)) return false;

        // 2. バーチャル空間（仮想盤面）を作成し、移動後に自分が王手されるかチェック（自爆手禁止）
        String[][] vGrid = cloneGrid(grid);
        vGrid[toR][toC] = vGrid[fromR][fromC];
        vGrid[fromR][fromC] = "";
        
        if (isKingInCheck(vGrid, player)) {
            return false; // 移動した結果、自分の王が王手されてしまうなら不可
        }

        return true; 
    }

    /**
     * 指定のマスに手札から召喚できるかチェックするメイン関数
     */
    public boolean isValidSpawn(String pieceId, int toR, int toC) {
        // 盤外チェック、またはすでに駒が存在する場合は召喚不可
        if (toR < 0 || toR >= 7 || toC < 0 || toC >= 7 || !grid[toR][toC].isEmpty()) return false;

        // 召喚ルール：自陣2列以内制限
        if ("P1".equals(currentTurn)) {
            if (toR < 5) return false; // 先手は下2列 (行5, 6)
        } else {
            if (toR > 1) return false; // 後手は上2列 (行0, 1)
        }

        // 二歩チェック (未成の歩 "FU" のみ対象、と金 "TO" はセーフ)
        if ("FU".equals(pieceId)) {
            for (int r = 0; r < 7; r++) {
                if (grid[r][toC].equals(currentTurn + "_FU")) {
                    return false; // 同一縦列に既に自分の歩があるため禁止
                }
            }
        }

        // バーチャル空間で「召喚後に自分が王手状態のままになっていないか」チェック
        String[][] vGrid = cloneGrid(grid);
        vGrid[toR][toC] = currentTurn + "_" + pieceId;
        if (isKingInCheck(vGrid, currentTurn)) {
            return false; 
        }

        // 打ち歩詰めチェック
        if ("FU".equals(pieceId)) {
            String enemy = "P1".equals(currentTurn) ? "P2" : "P1";
            // 仮想盤面で相手が王手状態、かつ、相手に合法手が1つもない（詰み）状態か調べる
            if (isKingInCheck(vGrid, enemy) && isCheckmate(vGrid, enemy)) {
                return false; // 打ち歩詰めになるため召喚禁止
            }
        }

        return true;
    }


    /* ====================================================================
     *  ヘルパー関数：バーチャル盤面判定アルゴリズム群
     * ==================================================================== */

    /**
     * 盤面配列をディープコピー（複製）する
     */
    private String[][] cloneGrid(String[][] original) {
        String[][] copy = new String[7][7];
        for (int i = 0; i < 7; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, 7);
        }
        return copy;
    }

    /**
     * 指定したプレイヤーの王が、指定盤面上で王手されているかをバーチャルに判定する
     */
    private boolean isKingInCheck(String[][] targetGrid, String player) {
        int kingR = -1, kingC = -1;
        // 王の位置を探す
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].equals(player + "_OU")) {
                    kingR = r; kingC = c; break;
                }
            }
        }
        if (kingR == -1) return false; // 万が一王がいない場合は偽

        // 相手の全ての駒が、次の手でこの王の位置に移動できるかを走査
        String enemy = "P1".equals(player) ? "P2" : "P1";
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].startsWith(enemy)) {
                    if (isValidPieceTrajectory(targetGrid, r, c, kingR, kingC)) {
                        return true; // 1つでも王のマスへ到達できる敵駒があれば王手
                    }
                }
            }
        }
        return false;
    }

    /**
     * 指定盤面において、対象プレイヤーが「詰み（合法手が一切ない）」状態かバーチャルに判定する
     */
    private boolean isCheckmate(String[][] targetGrid, String player) {
        // 盤上の全ての自駒に対して、どこか1マスでも「王手を回避できる安全な移動先」があるか試す
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].startsWith(player)) {
                    for (int toR = 0; toR < 7; toR++) {
                        for (int toC = 0; toC < 7; toC++) {
                            // 自分の駒と同じ場所、または自駒の上は除外
                            if ((r == toR && c == toC) || targetGrid[toR][toC].startsWith(player)) continue;

                            // 駒の軌道が通るか
                            if (isValidPieceTrajectory(targetGrid, r, c, toR, toC)) {
                                // 仮想的に動かしてみる
                                String[][] vGrid = cloneGrid(targetGrid);
                                vGrid[toR][toC] = vGrid[r][c];
                                vGrid[r][c] = "";
                                
                                // この移動によって王手が解除される（または王手されない）なら、詰みではない！
                                if (!isKingInCheck(vGrid, player)) {
                                    return false; 
                                }
                            }
                        }
                    }
                }
            }
        }
        return true; // あらゆる移動をシミュレートしても王手を外せなければ詰み
    }

    /**
     * 駒ごとの「純粋な軌道と動きのルール」を判定する（障害物の飛び越しチェック含む）
     */
    private boolean isValidPieceTrajectory(String[][] currentGrid, int fromR, int fromC, int toR, int toC) {
        String pieceInfo = currentGrid[fromR][fromC];
        String player = pieceInfo.substring(0, 2);
        String pieceId = pieceInfo.substring(3); // "OU", "FU", "NY"(成香) など

        int dr = toR - fromR;
        int dc = toC - fromC;
        
        // プレイヤーの向きによる前方向の補正（P1は上が前(マイナス)、P2は下が前(プラス)）
        int dir = "P1".equals(player) ? -1 : 1;

        switch (pieceId) {
            case "OU": // 周囲1マス
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;

            case "FU": // 前1マス
                return dr == dir && dc == 0;

            case "KI": // 金：前、斜め前、横、後ろ（＝斜め後ろ以外）
            case "TO": // と金
            case "NY": // 成香
            case "NK": // 成桂
            case "NG": // 成銀
                if (Math.abs(dr) > 1 || Math.abs(dc) > 1) return false;
                if (dr == -dir && Math.abs(dc) == 1) return false; // 斜め後ろは不可
                return true;

            case "GI": // 銀：前、斜め前、斜め後ろ（＝横と後ろ以外）
                if (Math.abs(dr) > 1 || Math.abs(dc) > 1) return false;
                if (dr == 0 && Math.abs(dc) == 1) return false; // 真横は不可
                if (dr == -dir && dc == 0) return false;        // 真後ろは不可
                return true;

            case "KE": // 桂馬：前方2マス ＋ 左右1マス
                return dr == (dir * 2) && Math.abs(dc) == 1;

            case "KY": // 香車：前方に何マスでも（障害物飛び越し不可）
                if (dc != 0) return false;
                if ((dir == -1 && dr >= 0) || (dir == 1 && dr <= 0)) return false; // 逆走不可
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "HI": // 飛車：直線（縦横）に何マスでも
                if (dr != 0 && dc != 0) return false;
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "RY": // 龍王：飛車 ＋ 周囲1マス
                if (dr == 0 || dc == 0) return isPathClear(currentGrid, fromR, fromC, toR, toC);
                return Math.abs(dr) == 1 && Math.abs(dc) == 1;

            case "KA": // 角行：斜めに何マスでも
                if (Math.abs(dr) != Math.abs(dc)) return false;
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "UM": // 龍馬：角 ＋ 周囲1マス
                if (Math.abs(dr) == Math.abs(dc)) return isPathClear(currentGrid, fromR, fromC, toR, toC);
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
        }
        return false;
    }

    /**
     * 直線・斜め移動時に、出発点から到達点の「途中」に他の駒がないかチェック（飛び越し禁止）
     */
    private boolean isPathClear(String[][] currentGrid, int fromR, int fromC, int toR, int toC) {
        int stepR = Integer.compare(toR, fromR);
        int stepC = Integer.compare(toC, fromC);
        
        int r = fromR + stepR;
        int c = fromC + stepC;
        
        // 到達マスの「手前」までループを回す
        while (r != toR || c != toC) {
            if (!currentGrid[r][c].isEmpty()) {
                return false; // 途中に駒があれば遮断
            }
            r += stepR;
            c += stepC;
        }
        return true;
    }


    /* ====================================================================
     *  移動・召喚後の後処理
     * ==================================================================== */
    
    public void handlePostMoveProcess(int fromR, int fromC, int toR, int toC) {
        String movingPiece = grid[fromR][fromC];
        String targetPiece = grid[toR][toC];
        String player = movingPiece.substring(0, 2);
        String pieceId = movingPiece.substring(3);

        // 捕獲処理
        if (!targetPiece.isEmpty()) {
            String enemyPieceId = targetPiece.split("_")[1];
            // 成り駒（TO, NY, NK, NG, RY, UM）を元の駒に戻す
            if ("TO".equals(enemyPieceId)) enemyPieceId = "FU";
            else if ("NY".equals(enemyPieceId)) enemyPieceId = "KY";
            else if ("NK".equals(enemyPieceId)) enemyPieceId = "KE";
            else if ("NG".equals(enemyPieceId)) enemyPieceId = "GI";
            else if ("RY".equals(enemyPieceId)) enemyPieceId = "HI";
            else if ("UM".equals(enemyPieceId)) enemyPieceId = "KA";

            getHand(currentTurn).add(enemyPieceId);
        }

        // 成り判定 (敵陣2列：P1なら行0,1 / P2なら行5,6)
        boolean isPromotionZone = "P1".equals(player) ? (toR <= 1 || fromR <= 1) : (toR >= 5 || fromR >= 5);
        
        // ★【修正】すでに成っている駒（TO, NY, NK, NG, RY, UM）や「王・金」は成れないように除外
        boolean canPromote = "FU".equals(pieceId) || "KY".equals(pieceId) || "KE".equals(pieceId) || 
                             "GI".equals(pieceId) || "HI".equals(pieceId) || "KA".equals(pieceId);

        if (isPromotionZone && canPromote) {
            // 歩・香・桂が最奥で行き所をなくす場合は強制成り、それ以外は任意
            boolean forcePromote = false;
            if ("FU".equals(pieceId) || "KY".equals(pieceId)) {
                forcePromote = "P1".equals(player) ? (toR == 0) : (toR == 6);
            } else if ("KE".equals(pieceId)) {
                forcePromote = "P1".equals(player) ? (toR <= 1) : (toR >= 5);
            }

            if (forcePromote) {
                movingPiece = promotePiece(player, pieceId);
            } else {
                int choice = javax.swing.JOptionPane.showConfirmDialog(null, "成りますか？", "成り選択", javax.swing.JOptionPane.YES_NO_OPTION);
                if (choice == javax.swing.JOptionPane.YES_OPTION) {
                    movingPiece = promotePiece(player, pieceId);
                }
            }
        }

        grid[toR][toC] = movingPiece;
        grid[fromR][fromC] = "";
    }

    // ★【追加】外部（MS_Offline2P）から現在の盤面での詰み判定を呼べるようにパブリックなラッパーを追加
    public boolean checkCheckmateForPlayer(String player) {
        return isCheckmate(this.grid, player);
    }
    
    private String promotePiece(String player, String pieceId) {
        if ("FU".equals(pieceId)) return player + "_TO";
        if ("KY".equals(pieceId)) return player + "_NY";
        if ("KE".equals(pieceId)) return player + "_NK";
        if ("GI".equals(pieceId)) return player + "_NG";
        if ("HI".equals(pieceId)) return player + "_RY";
        if ("KA".equals(pieceId)) return player + "_UM";
        return player + "_" + pieceId;
    }

    public void handlePostSpawnProcess(int index, int toR, int toC) {
        String pieceId = getHand(currentTurn).remove(index);
        grid[toR][toC] = currentTurn + "_" + pieceId;
    }
}
