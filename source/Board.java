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

    /**
     * ゲーム開始時の初期化処理
     */
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

    /**
     * 山札からカードを1枚ドローする
     */
    public void drawCard(String player) {
        List<String> deck = "P1".equals(player) ? p1Deck : p2Deck;
        List<String> hand = "P1".equals(player) ? p1Hand : p2Hand;
        if (!deck.isEmpty()) {
            hand.add(deck.remove(0));
        }
    }

    /**
     * 選択状態を完全にクリアする
     */
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
        if (fromR < 0 || fromR >= 7 || fromC < 0 || fromC >= 7 || toR < 0 || toR >= 7 || toC < 0 || toC >= 7) return false;
        
        String piece = grid[fromR][fromC];
        if (piece.isEmpty()) return false;
        
        String player = piece.substring(0, 2); 
        String target = grid[toR][toC];
        
        if (!target.isEmpty() && target.startsWith(player)) return false;

        if (!isValidPieceTrajectory(grid, fromR, fromC, toR, toC)) return false;

        // バーチャル空間（仮想盤面）を作成し、移動後に自分が王手されるかチェック（自爆手禁止）
        String[][] vGrid = cloneGrid(grid);
        vGrid[toR][toC] = vGrid[fromR][fromC];
        vGrid[fromR][fromC] = "";
        
        if (isKingInCheck(vGrid, player)) {
            return false; 
        }

        return true; 
    }

    /**
     * 指定のマスに手札から召喚できるかチェックするメイン関数
     */
    public boolean isValidSpawn(String pieceId, int toR, int toC) {
        if (toR < 0 || toR >= 7 || toC < 0 || toC >= 7 || !grid[toR][toC].isEmpty()) return false;

        if ("P1".equals(currentTurn)) {
            if (toR < 5) return false; 
        } else {
            if (toR > 1) return false; 
        }

        if ("FU".equals(pieceId)) {
            for (int r = 0; r < 7; r++) {
                if (grid[r][toC].equals(currentTurn + "_FU")) {
                    return false; 
                }
            }
        }

        String[][] vGrid = cloneGrid(grid);
        vGrid[toR][toC] = currentTurn + "_" + pieceId;
        if (isKingInCheck(vGrid, currentTurn)) {
            return false; 
        }

        if ("FU".equals(pieceId)) {
            String enemy = "P1".equals(currentTurn) ? "P2" : "P1";
            if (isKingInCheck(vGrid, enemy) && isCheckmate(vGrid, enemy)) {
                return false; 
            }
        }

        return true;
    }


    /* ====================================================================
     *  ヘルパー関数：バーチャル盤面判定アルゴリズム群
     * ==================================================================== */

    private String[][] cloneGrid(String[][] original) {
        String[][] copy = new String[7][7];
        for (int i = 0; i < 7; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, 7);
        }
        return copy;
    }

    private boolean isKingInCheck(String[][] targetGrid, String player) {
        int kingR = -1, kingC = -1;
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].equals(player + "_OU")) {
                    kingR = r; kingC = c; break;
                }
            }
        }
        if (kingR == -1) return false; 

        String enemy = "P1".equals(player) ? "P2" : "P1";
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].startsWith(enemy)) {
                    if (isValidPieceTrajectory(targetGrid, r, c, kingR, kingC)) {
                        return true; 
                    }
                }
            }
        }
        return false;
    }

    private boolean isCheckmate(String[][] targetGrid, String player) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                if (targetGrid[r][c].startsWith(player)) {
                    for (int toR = 0; toR < 7; toR++) {
                        for (int toC = 0; toC < 7; toC++) {
                            if ((r == toR && c == toC) || targetGrid[toR][toC].startsWith(player)) continue;

                            if (isValidPieceTrajectory(targetGrid, r, c, toR, toC)) {
                                String[][] vGrid = cloneGrid(targetGrid);
                                vGrid[toR][toC] = vGrid[r][c];
                                vGrid[r][c] = "";
                                
                                if (!isKingInCheck(vGrid, player)) {
                                    return false; 
                                }
                            }
                        }
                    }
                }
            }
        }
        return true; 
    }

    private boolean isValidPieceTrajectory(String[][] currentGrid, int fromR, int fromC, int toR, int toC) {
        String pieceInfo = currentGrid[fromR][fromC];
        String player = pieceInfo.substring(0, 2);
        String pieceId = pieceInfo.substring(3); 

        int dr = toR - fromR;
        int dc = toC - fromC;
        int dir = "P1".equals(player) ? -1 : 1;

        switch (pieceId) {
            case "OU": 
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;

            case "FU": 
                return dr == dir && dc == 0;

            case "KI": 
            case "TO": 
            case "NY": 
            case "NK": 
            case "NG": 
                if (Math.abs(dr) > 1 || Math.abs(dc) > 1) return false;
                if (dr == -dir && Math.abs(dc) == 1) return false; 
                return true;

            case "GI": 
                if (Math.abs(dr) > 1 || Math.abs(dc) > 1) return false;
                if (dr == 0 && Math.abs(dc) == 1) return false; 
                if (dr == -dir && dc == 0) return false;        
                return true;

            case "KE": 
                return dr == (dir * 2) && Math.abs(dc) == 1;

            case "KY": 
                if (dc != 0) return false;
                if ((dir == -1 && dr >= 0) || (dir == 1 && dr <= 0)) return false; 
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "HI": 
                if (dr != 0 && dc != 0) return false;
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "RY": 
                if (dr == 0 || dc == 0) return isPathClear(currentGrid, fromR, fromC, toR, toC);
                return Math.abs(dr) == 1 && Math.abs(dc) == 1;

            case "KA": 
                if (Math.abs(dr) != Math.abs(dc)) return false;
                return isPathClear(currentGrid, fromR, fromC, toR, toC);

            case "UM": 
                if (Math.abs(dr) == Math.abs(dc)) return isPathClear(currentGrid, fromR, fromC, toR, toC);
                return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
        }
        return false;
    }

    private boolean isPathClear(String[][] currentGrid, int fromR, int fromC, int toR, int toC) {
        int stepR = Integer.compare(toR, fromR);
        int stepC = Integer.compare(toC, fromC);
        int r = fromR + stepR;
        int c = fromC + stepC;
        
        while (r != toR || c != toC) {
            if (!currentGrid[r][c].isEmpty()) return false;
            r += stepR;
            c += stepC;
        }
        return true;
    }


    /* ====================================================================
     *  移動・召喚後の後処理
     * ==================================================================== */
    
    /**
     * 移動後の駒の配置、捕獲、成り判定を処理する
     */
    public void handlePostMoveProcess(int fromR, int fromC, int toR, int toC) {
        String movingPiece = grid[fromR][fromC];
        String targetPiece = grid[toR][toC];
        String player = movingPiece.substring(0, 2);
        String pieceId = movingPiece.substring(3);

        // 捕獲処理
        if (!targetPiece.isEmpty()) {
            String enemyPieceId = targetPiece.split("_")[1];
            // 成り駒を元の駒に戻して手札に加える
            if ("TO".equals(enemyPieceId)) enemyPieceId = "FU";
            else if ("NY".equals(enemyPieceId)) enemyPieceId = "KY";
            else if ("NK".equals(enemyPieceId)) enemyPieceId = "KE";
            else if ("NG".equals(enemyPieceId)) enemyPieceId = "GI";
            else if ("RY".equals(enemyPieceId)) enemyPieceId = "HI";
            else if ("UM".equals(enemyPieceId)) enemyPieceId = "KA";

            getHand(currentTurn).add(enemyPieceId);
        }

        // 成り判定領域の算出 (敵陣2列：P1なら行0,1 / P2なら行5,6)
        boolean isPromotionZone = "P1".equals(player) ? (toR <= 1 || fromR <= 1) : (toR >= 5 || fromR >= 5);
        
        // ★【修正】すでに成っている駒（TO, NY等）や「王(OU)・金(KI)」は成れないように事前に弾く
        boolean canPromote = "FU".equals(pieceId) || "KY".equals(pieceId) || "KE".equals(pieceId) || 
                             "GI".equals(pieceId) || "HI".equals(pieceId) || "KA".equals(pieceId);

        if (isPromotionZone && canPromote) {
            // 行き所のない駒の強制成り判定
            boolean forcePromote = false;
            if ("FU".equals(pieceId) || "KY".equals(pieceId)) {
                forcePromote = "P1".equals(player) ? (toR == 0) : (toR == 6);
            } else if ("KE".equals(pieceId)) {
                forcePromote = "P1".equals(player) ? (toR <= 1) : (toR >= 5);
            }

            if (forcePromote) {
                movingPiece = promotePiece(player, pieceId);
            } else {
                // すでに成っている駒はここを通らないため、「成りますか？」ダイアログは表示されなくなります
                int choice = javax.swing.JOptionPane.showConfirmDialog(null, "成りますか？", "成り選択", javax.swing.JOptionPane.YES_NO_OPTION);
                if (choice == javax.swing.JOptionPane.YES_OPTION) {
                    movingPiece = promotePiece(player, pieceId);
                }
            }
        }

        grid[toR][toC] = movingPiece;
        grid[fromR][fromC] = "";
    }

    /**
     * 外部から対象プレイヤーの詰み状況をチェックするための公開メソッド
     */
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
