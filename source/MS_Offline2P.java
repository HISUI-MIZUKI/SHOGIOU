package source;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

/**
 * 画面：オフライン2人対戦用のメインパネル。
 * ハイライト機能、手札のスクロール機能、Escapeキー等によるキャンセル機能を実装。
 */
public class MS_Offline2P extends JPanel {
    private static MS_Offline2P instance;
    
    private JPanel boardPanel;          // 7x7の将棋盤
    private JPanel activeHandPanel;     // 手札のボタンを並べるパネル
    private JScrollPane handScrollPane; // 手札用スクロールペイン（見切れ防止）
    private JLabel statusLabel;         // 上部の状態指示ラベル
    
    private String turnPhase = "WAIT_ACTION"; // "WAIT_ACTION" または "SPAWN"

    private MS_Offline2P() {
        setLayout(new BorderLayout());

        // フォーカスを持たせてキーボード入力を受け取れるようにする
        setFocusable(true);
        
        // エスケープキーによる手札選択キャンセルのリスナー登録
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelSelectionAndRefresh();
                }
            }
        });

        statusLabel = new JLabel("対戦準備中...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        add(statusLabel, BorderLayout.NORTH);

        boardPanel = new JPanel(new GridLayout(7, 7));
        add(boardPanel, BorderLayout.CENTER);

        // 南側（下部）のコンテナ
        JPanel southContainer = new JPanel(new BorderLayout());
        activeHandPanel = new JPanel(); 
        
        // ★手札ボタンの並び（activeHandPanel）の上下左右に十分な余白を設定
        activeHandPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // 手札用スクロールペインの作成
        handScrollPane = new JScrollPane(activeHandPanel);
        handScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        handScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        
        // ★【サイズ潰れ対策】スクロールペインが潰れないよう、最小・推奨・最大サイズをすべて「高さ100ピクセル」に固定
        java.awt.Dimension handSize = new java.awt.Dimension(800, 100);
        handScrollPane.setPreferredSize(handSize);
        handScrollPane.setMinimumSize(handSize);
        handScrollPane.setMaximumSize(handSize);
        
        southContainer.add(handScrollPane, BorderLayout.CENTER);

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

    /**
     * 画面が開かれたときの初期デッキ選択ダイアログとゲーム開始処理
     */
    public void onScreenOpened() {
        // キー入力を受け付けるためにフォーカスを要求
        requestFocusInWindow();

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
        
        statusLabel.setText(turnStr + "の手番：駒を動かすか、手札を選んで召喚してください。(Escでキャンセル)");
        refreshUI();
    }

    /**
     * 手札選択のキャンセル処理（Escキーや同じ手札の再クリック時に使用）
     */
    private void cancelSelectionAndRefresh() {
        Board board = Board.getInstance();
        String turnStr = "P1".equals(board.getCurrentTurn()) ? "先手(P1)" : "後手(P2)";
        
        turnPhase = "WAIT_ACTION";
        board.clearSelection();
        statusLabel.setText(turnStr + "の手番：駒を動かすか、手札を選んで召喚してください。");
        refreshUI();
        
        // 再度キー入力を受け付けるためにフォーカスを確保
        requestFocusInWindow();
    }

    /**
     * 画面全体の描画更新ロジック
     */
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

                // このマスが移動・召喚の候補地(ハイライト対象)かどうかの判定
                boolean isTarget = false;
                if ("SPAWN".equals(turnPhase)) {
                    String pieceId = board.getHand(turn).get(board.getSelectedHandIndex());
                    isTarget = board.isValidSpawn(pieceId, row, col) && pieceInfo.isEmpty();
                } else if ("WAIT_ACTION".equals(turnPhase) && board.getSelectedRow() != -1) {
                    isTarget = board.isValidMove(board.getSelectedRow(), board.getSelectedCol(), row, col);
                }

                // マスの背景色割り当て
                if (row == board.getSelectedRow() && col == board.getSelectedCol()) {
                    cell.setBackground(Color.YELLOW); 
                } else if (isTarget) {
                    cell.setBackground(new Color(144, 238, 144)); // 候補地を薄緑色に
                } else if ((row + col) % 2 == 0) {
                    cell.setBackground(new Color(240, 217, 181)); 
                } else {
                    cell.setBackground(new Color(181, 136, 99));  
                }

                // ボタンの有効化 / 無効化制限
                if ("SPAWN".equals(turnPhase)) {
                    if (!isTarget) cell.setEnabled(false);
                } else if ("WAIT_ACTION".equals(turnPhase) && board.getSelectedRow() != -1) {
                    if (!isTarget && !(row == board.getSelectedRow() && col == board.getSelectedCol())) {
                        cell.setEnabled(false);
                    }
                } else {
                    if (pieceInfo.isEmpty() || !pieceInfo.startsWith(turn)) {
                        cell.setEnabled(false);
                    }
                }

                // 駒の描画処理
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

        // 2. 現在の手番プレイヤーの「手札」エリアの再描画
        activeHandPanel.removeAll();
        activeHandPanel.add(new JLabel(turnStr + " の手札 (山札残り: " + board.getDeck(turn).size() + "枚): "));
        
        List<String> hand = board.getHand(turn); 
        for (int i = 0; i < hand.size(); i++) {
            final int index = i;
            DB_Pieces.PieceData data = DB_Pieces.get(hand.get(i));
            JButton handButton = new JButton(data != null ? data.displayName : hand.get(i));
            
            // ★手札の文字を大きくし、ボタン自体のサイズもしっかり確保して見やすくする
            handButton.setFont(new Font("MS Gothic", Font.BOLD, 18));
            handButton.setPreferredSize(new java.awt.Dimension(70, 50)); 
            
            if (index == board.getSelectedHandIndex()) {
                handButton.setBackground(Color.YELLOW);
            }

            handButton.addActionListener(e -> {
                // すでに選択中の手札をもう一度クリックした場合は選択をキャンセルする
                if (index == board.getSelectedHandIndex()) {
                    cancelSelectionAndRefresh();
                    return;
                }

                if ("WAIT_ACTION".equals(turnPhase) || "SPAWN".equals(turnPhase)) {
                    turnPhase = "SPAWN";
                    board.clearSelection(); 
                    board.setSelectedHandIndex(index);
                    statusLabel.setText("【召喚モード】薄緑色のマスを選択して召喚してください。(もう一度クリック、またはEscでキャンセル)");
                    refreshUI(); 
                    
                    // ボタンクリック後にフォーカスが奪われるのを防ぎ、Escを効かせるため
                    requestFocusInWindow();
                }
            });
            activeHandPanel.add(handButton);
        }

        boardPanel.revalidate();
        boardPanel.repaint();
        activeHandPanel.revalidate();
        activeHandPanel.repaint();
    }

    /**
     * 将棋盤のマスがクリックされた際の判定処理
     */
    private void handleBoardClick(int r, int c) {
        Board board = Board.getInstance();

        if ("SPAWN".equals(turnPhase)) {
            board.handlePostSpawnProcess(board.getSelectedHandIndex(), r, c);
            endTurn();
            return;
        }

        if ("WAIT_ACTION".equals(turnPhase)) {
            if (board.getSelectedRow() != -1 && board.getSelectedCol() != -1) {
                int fromR = board.getSelectedRow();
                int fromC = board.getSelectedCol();

                if (fromR == r && fromC == c) {
                    cancelSelectionAndRefresh();
                } else {
                    board.handlePostMoveProcess(fromR, fromC, r, c);
                    endTurn();
                }
            } else {
                board.setSelectedCell(r, c);
                statusLabel.setText("【移動モード】薄緑色のマスを選択して移動してください。");
                refreshUI();
            }
        }
        
        // 操作後にキーボード入力を通すためフォーカスを戻す
        requestFocusInWindow();
    }

    /**
     * ターン終了および詰み判定処理
     */
    private void endTurn() {
        Board board = Board.getInstance();
        
        // 次の手番プレイヤーの算出
        String nextPlayer = "P1".equals(board.getCurrentTurn()) ? "P2" : "P1";
        String nextPlayerStr = "P1".equals(nextPlayer) ? "先手(P1)" : "後手(P2)";
        String winnerStr = "P1".equals(board.getCurrentTurn()) ? "先手(P1)" : "後手(P2)";

        // 手番交代前に、次に回るプレイヤーが「詰んで」いるかをバーチャルシミュレーション
        if (board.checkCheckmateForPlayer(nextPlayer)) {
            refreshUI(); // 最後の決定打を画面に表示するために一度描画を反映
            JOptionPane.showMessageDialog(this, nextPlayerStr + "の王が詰みました！\n" + winnerStr + "の勝利です！", "ゲーム終了", JOptionPane.INFORMATION_MESSAGE);
            
            // タイトル画面（HOME）へ戻る
            Singleton_Manager.getInstance().changeScreen("HOME");
            return;
        }

        // 詰んでいなければ通常のターン交代へ
        board.switchTurn(); 
        startNewTurn();
    }
}
