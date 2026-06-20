package source.Main_ScreenFlow;

import javax.swing.*;
import java.awt.*;

public class SF_Game_Offline extends ScreenFlow {
    private JFrame frame;

    public SF_Game_Offline(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public void Init() {
        // 現在の画面をクリア
        frame.getContentPane().removeAll();

        // 7x7の盤面用パネル
        JPanel boardPanel = new JPanel(new GridLayout(7, 7));
        
        for (int i = 0; i < 49; i++) {
            JButton cell = new JButton();
            // 交互に色を変える
            if ((i / 7 + i % 7) % 2 == 0) {
                cell.setBackground(new Color(245, 222, 179)); // 木目色
            } else {
                cell.setBackground(new Color(210, 180, 140));
            }
            boardPanel.add(cell);
        }

        frame.add(boardPanel);
        frame.revalidate();
        frame.repaint();
    }

    @Override
    public void Run() {
        System.out.println("ゲーム画面実行中");
    }

    @Override
    public void Final() {
        System.out.println("ゲーム画面終了");
    }
}