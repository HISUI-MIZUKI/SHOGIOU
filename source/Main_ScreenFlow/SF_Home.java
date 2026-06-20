package source.Main_ScreenFlow;

import javax.swing.*;

public class SF_Home extends ScreenFlow {
    private JFrame frame;

    public SF_Home(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public void Init() {
        frame.getContentPane().removeAll();
        JButton pvpButton = new JButton("オフライン対戦(PvP)");
        
        // ボタンを押した時の処理：SF_Manager経由で画面を切り替える
        pvpButton.addActionListener(e -> {
            SF_Manager.getInstance().changeScreen(new SF_Game_Offline(frame));
        });

        frame.add(pvpButton);
        frame.revalidate();
        frame.repaint();
    }
}