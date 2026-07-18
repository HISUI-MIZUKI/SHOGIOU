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
 * ターン開始時に自動ドローを行い、その後「移動」か「召喚」を自由に選択できるルールに修正しました。
 */
public class MS_Offline2P extends JPanel {
    private static MS_Offline2P instance;
    
    private JPanel boardPanel;          // 7x7の将棋盤
    private JPanel activeHandPanel;     // 現在の手番プレイヤーの手札エリア
    private JLabel statusLabel;         // 上部の状態指示ラベル
    
    /**
     * 現在のターンのアクション状態を管理するステート。
     * "WAIT_ACTION" : ターン開始（ドロー後）の状態。駒の移動か、手札の選択（召喚）を待っている状態。
     * "SPAWN"       : 手札が選択され、召喚先のマスを選択している状態。
     */
    private String turnPhase = "WAIT_ACTION"; 

    private MS_Offline2P() {
        setLayout(new BorderLayout());

        // 1. 画面上部：ステータス表示エリア
        statusLabel = new JLabel("対戦準備中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        // 2. 画面中央：7x7 グリッドレイアウトの盤面エリア
        boardPanel = new JPanel(new GridLayout(7, 7));
        add(boardPanel, BorderLayout.CENTER);

        // 3. 画面下部：手札と操作メニューをまとめるコンテナ
        JPanel southContainer = new JPanel(new BorderLayout());
        activeHandPanel = new JPanel(); 
        southContainer.add(activeHandPanel, BorderLayout.CENTER);

        // メニューに戻るボタン
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

        // Board側のゲーム初期化（※setupGame内での自動ドローは実装に合わせて調整してください）
        Board.getInstance().setupGame(p1Deck, p2Deck);

        // 最初のターンを開始
        startNewTurn();
    }

    /**
     * ターン開始処理：ターンが回ってきた瞬間に山札から1枚自動ドローします。
     */
    private void startNewTurn() {
        Board board = Board.getInstance();
        String turn = board.getCurrentTurn();
        String turnStr = "P1".equals(turn) ? "先手(P1)" : "後手(P2)";

        // 【新ルール】ターン開始時に山札から1枚自動でドロー
        board.drawCard(turn);

        // 状態をアクション選択待ちにリセット
        turnPhase = "WAIT_ACTION";
        board.clearSelection(); 
        
        statusLabel.setText(turnStr + "の手番（1枚ドローしました）：駒を動かすか、手札を選んで召喚してください。");
        
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

                // マスハイライト処理
                if (r == board.getSelectedRow() && c == board.getSelectedCol()) {
                    cell.setBackground(Color.YELLOW); 
                } else if ((r + c) % 2 == 0) {
                    cell.setBackground(new Color(240, 217, 181)); 
                } else {
                    cell.setBackground(new Color(181, 136, 99));  
                }

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

        // 2. 現在の手番プレイヤーの「手札」のみの再描画
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

            // 手札ボタンのイベント：選択すると召喚モードへ切り替える
            handButton.addActionListener(e -> {
                // 既に盤面の駒を選択して移動させようとしている時は手札選択を上書きキャンセルできるようにする
                if ("WAIT_ACTION".equals(turnPhase) || "SPAWN".equals(turnPhase)) {
                    turnPhase = "SPAWN";
                    board.clearSelection(); // 盤面選択（黄色マス）があれば解除
                    board.setSelectedHandIndex(index);
                    statusLabel.setText("【召喚モード】自陣2列の空きマスを選択して召喚してください。");
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
        String clickedPiece = board.getPieceAt(r, c);

        // ==========================================
        // ケースA: 【召喚モード】時の処理
        // ==========================================
        if ("SPAWN".equals(turnPhase)) {
            if (board.getSelectedHandIndex() == -1) {
                JOptionPane.showMessageDialog(this, "まず手札から召喚したい駒を選択してください。");
                return;
            }
            
            // 召喚位置の検証
            if (board.isValidSpawn(board.getHand(turn).get(board.getSelectedHandIndex()), r, c) && clickedPiece.isEmpty()) {
                board.handlePostSpawnProcess(board.getSelectedHandIndex(), r, c);
                endTurn(); 
            } else {
                JOptionPane.showMessageDialog(this, "そこには召喚できません！位置を再選択するか、手札を選び直してください。");
            }
            return;
        }

        // ==========================================
        // ケースB: 【通常・移動モード】時の処理
        // ==========================================
        if ("WAIT_ACTION".equals(turnPhase)) {
            // すでに移動元の駒を選択済みの状態（＝移動先を決定するクリック）
            if (board.getSelectedRow() != -1 && board.getSelectedCol() != -1) {
                int fromR = board.getSelectedRow();
                int fromC = board.getSelectedCol();

                // 自分の駒をもう一度クリックしたら選択キャンセル
                if (fromR == r && fromC == c) {
                    board.clearSelection();
                    refreshUI();
                    return;
                }

                // 移動先として有効かチェック
                if (board.isValidMove(fromR, fromC, r, c)) {
                    board.handlePostMoveProcess(fromR, fromC, r, c);
                    endTurn(); 
                } else {
                    JOptionPane.showMessageDialog(this, "そこには移動できません！移動先を再選択してください。");
                }
            } 
            // まだ何も選んでいない状態（＝動かしたい自分の駒を選択するクリック）
            else {
                if (!clickedPiece.isEmpty() && clickedPiece.startsWith(turn)) {
                    board.setSelectedCell(r, c);
                    statusLabel.setText("【移動モード】移動先のマスを選択してください。");
                    refreshUI(); 
                }
            }
        }
    }

    private void endTurn() {
        Board board = Board.getInstance();
        board.switchTurn(); 
        
        // 次のターンの開始処理（自動ドロー）へ
        startNewTurn();
    }
}
