package source;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import java.awt.Font;
import java.net.URL;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * 画面：オフライン2人対戦用のメインパネル。
 * 選択した駒の移動・召喚可能マスを薄緑色にハイライトし、操作を限定する機能を実装。
 */
public class MS_Offline2P extends JPanel {
    private static MS_Offline2P instance;
    
    private JPanel boardPanel;          // 7x7の将棋盤
    private JPanel activeHandPanel;     // 現在の手番プレイヤーの手札エリア
    private JLabel statusLabel;         // 上部の状態指示ラベル
    
    private String turnPhase = "WAIT_ACTION"; 

    private MS_Offline2P() {
        setLayout(new BorderLayout());

        statusLabel = new JLabel("対戦準備中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new JPanel(new GridLayout(7, 7));
        add(boardPanel, BorderLayout.CENTER);

        JPanel southContainer = new JPanel(new BorderLayout());
        activeHandPanel = new JPanel(); 
        southContainer.add(activeHandPanel, BorderLayout.CENTER);

        JButton backButton = new JButton("メニューへ戻る");
        backButton.addActionListener(e -> Singleton_Manager.getInstance().changeScreen("HOME"));
        southContainer.add(backButton, BorderLayout.SOUTH);
        
        add(southContainer, BorderLayout.SOUTH);
    }

    public static synchronized MS_Offline2P getInstance() {
        if (instance == null) {
            instance = new MS_Offline2P();
        }
        return instance;
    }

    public void onScreenOpened() {
        String[] options = {"飛車デッキ", "角デッキ"};
        int p1Choice = JOptionPane.showOptionDialog(this, "先手(P1)のデッキを選んでください", "デッキ選択", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        int p2Choice = JOptionPane.showOptionDialog(this, "後手(P2)のデッキを選んでください", "デッキ選択", 
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        String p1Deck = (p1Choice == 0) ? "HI" : "KA";
        String p2Deck = (p2Choice == 0) ? "HI" : "KA";

        Board.getInstance().setupGame(p1Deck, p2Deck);
        startNewTurn();
    }

    private void startNewTurn() {
        Board board = Board.getInstance();
        String turn = board.getCurrentTurn();
        String turnStr = "P1".equals(turn) ? "先手(P1)" : "後手(P2)";

        board.drawCard(turn);

        turnPhase = "WAIT_ACTION";
        board.clearSelection(); 
        
        statusLabel.setText(turnStr + "の手番：駒を動かすか、手札を選んで召喚してください。");
        refreshUI();
    }

    public void refreshUI() {
        Board board = Board.getInstance();
        String turn = board.getCurrentTurn();
        String turnStr = "P1".equals(turn) ? "先手(P1)" : "後手(P2)";

        // 1. 7x7 盤面の再描画
        boardPanel.removeAll();
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                final int row = r;
                final int col = c;
                String pieceInfo = board.getPieceAt(r, c);
                JButton cell = new JButton();

                // --- 【追加】このマスが「選択可能（ハイライト対象）」かどうかの判定 ---
                boolean isTarget = false;
                if ("SPAWN".equals(turnPhase)) {
                    // 召喚モード：手札の駒がここに召喚可能か
                    String pieceId = board.getHand(turn).get(board.getSelectedHandIndex());
                    isTarget = board.isValidSpawn(pieceId, row, col) && pieceInfo.isEmpty();
                } else if ("WAIT_ACTION".equals(turnPhase) && board.getSelectedRow() != -1) {
                    // 移動モード（駒選択済）：選択中の駒がここに移動可能か
                    isTarget = board.isValidMove(board.getSelectedRow(), board.getSelectedCol(), row, col);
                }

                // --- マスの色管理 ---
                if (row == board.getSelectedRow() && col == board.getSelectedCol()) {
                    cell.setBackground(Color.YELLOW); // 選択中の自分の駒
                } else if (isTarget) {
                    cell.setBackground(new Color(144, 238, 144)); // ★移動・召喚候補地を「薄緑色」に
                } else if ((row + col) % 2 == 0) {
                    cell.setBackground(new Color(240, 217, 181)); 
                } else {
                    cell.setBackground(new Color(181, 136, 99));  
                }

                // --- 【追加】その場所のみ押せるように制限する（有効化/無効化の制御） ---
                if ("SPAWN".equals(turnPhase)) {
                    // 召喚モード時は、薄緑のマス（候補地）以外はクリック不可にする
                    if (!isTarget) {
                        cell.setEnabled(false);
                    }
                } else if ("WAIT_ACTION".equals(turnPhase) && board.getSelectedRow() != -1) {
                    // 移動モードで駒選択済みの時は、薄緑のマスか、自分自身のマス（キャンセル用）以外は不可
                    if (!isTarget && !(row == board.getSelectedRow() && col == board.getSelectedCol())) {
                        cell.setEnabled(false);
                    }
                } else {
                    // まだ何も選択していない時は、空マスや相手の駒のマスを触れないようにする（自駒のみ選択可）
                    if (pieceInfo.isEmpty() || !pieceInfo.startsWith(turn)) {
                        cell.setEnabled(false);
                    }
                }

                // 駒の文字・画像描画処理
                if (!pieceInfo.isEmpty()) {
                    String[] parts = pieceInfo.split("_");
                    String player = parts[0]; 
                    String pieceId = parts[1]; 
                    
                    DB_Pieces.PieceData data = DB_Pieces.get(pieceId);
                    if (data != null) {
                        URL imgURL = getClass().getResource(data.imagePath);
                        if (imgURL != null) {
                            cell.setIcon(new ImageIcon(imgURL));
                        } else {
                            cell.setFont(new Font("MS Gothic", Font.BOLD, 18));
                            if ("P1".equals(player)) {
                                cell.setText(data.displayName);
                                cell.setForeground(Color.BLACK);
                            } else {
                                cell.setText("▽" + data.displayName);
                                cell.setForeground(new Color(150, 0, 0));
                            }
                        }
                    }
                }

                cell.addActionListener(e -> handleBoardClick(row, col));
                boardPanel.add(cell);
            }
        }

        // 2. 現在の手番プレイヤーの「手札」の再描画
        activeHandPanel.removeAll();
        activeHandPanel.add(new JLabel(turnStr + " の手札 (山札残り: " + board.getDeck(turn).size() + "枚): "));
        
        List<String> hand = board.getHand(turn); 
        for (int i = 0; i < hand.size(); i++) {
            final int index = i;
            DB_Pieces.PieceData data = DB_Pieces.get(hand.get(i));
            JButton handButton = new JButton(data != null ? data.displayName : hand.get(i));
            
            if (index == board.getSelectedHandIndex()) {
                handButton.setBackground(Color.YELLOW);
            }

            handButton.addActionListener(e -> {
                if ("WAIT_ACTION".equals(turnPhase) || "SPAWN".equals(turnPhase)) {
                    turnPhase = "SPAWN";
                    board.clearSelection(); 
                    board.setSelectedHandIndex(index);
                    statusLabel.setText("【召喚モード】薄緑色のマスを選択して召喚してください。");
                    refreshUI(); 
                }
            });
            activeHandPanel.add(handButton);
        }

        boardPanel.revalidate();
        boardPanel.repaint();
        activeHandPanel.revalidate();
        activeHandPanel.repaint();
    }

    private void handleBoardClick(int r, int c) {
        Board board = Board.getInstance();
        String turn = board.getCurrentTurn();

        // すでに UI 側で有効なマス（または解除マス）しか叩けないよう制限しているため、
        // ここでは単純に状態に応じた確定処理を行うだけで安全に動作します。

        if ("SPAWN".equals(turnPhase)) {
            // 召喚処理の実行
            board.handlePostSpawnProcess(board.getSelectedHandIndex(), r, c);
            endTurn();
            return;
        }

        if ("WAIT_ACTION".equals(turnPhase)) {
            if (board.getSelectedRow() != -1 && board.getSelectedCol() != -1) {
                int fromR = board.getSelectedRow();
                int fromC = board.getSelectedCol();

                if (fromR == r && fromC == c) {
                    // 自分をもう一度クリックした場合は選択解除
                    board.clearSelection();
                    refreshUI();
                } else {
                    // 移動処理の実行
                    board.handlePostMoveProcess(fromR, fromC, r, c);
                    endTurn();
                }
            } else {
                // 最初に自分の駒を選択した時
                board.setSelectedCell(r, c);
                statusLabel.setText("【移動モード】薄緑色のマスを選択して移動してください。");
                refreshUI();
            }
        }
    }

private void endTurn() {
        Board board = Board.getInstance();
        
        // 次の手番のプレイヤー
        String nextPlayer = "P1".equals(board.getCurrentTurn()) ? "P2" : "P1";
        String nextPlayerStr = "P1".equals(nextPlayer) ? "先手(P1)" : "後手(P2)";
        String winnerStr = "P1".equals(board.getCurrentTurn()) ? "先手(P1)" : "後手(P2)";

        // ★【追加】手番が移る前に、次のプレイヤーが詰んでいるかをバーチャル空間で判定
        if (board.checkCheckmateForPlayer(nextPlayer)) {
            refreshUI(); // 最後の1手を盤面に反映させるため一度描画
            JOptionPane.showMessageDialog(this, nextPlayerStr + "の王が詰みました！\n" + winnerStr + "の勝利です！", "ゲーム終了", JOptionPane.INFORMATION_MESSAGE);
            
            // タイトル画面へ戻る
            Singleton_Manager.getInstance().changeScreen("HOME");
            return;
        }

        // 詰んでいなければ通常通り手番を交代して次のターンへ
        board.switchTurn(); 
        startNewTurn();
    }
}
