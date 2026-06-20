import javax.swing.JFrame;

import source.Main_ScreenFlow.*;

public class Main {
    public static void main(String[] args) {
    JFrame frame = new JFrame("将☆戯☆王");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(600, 600);
    frame.setVisible(true);

    SF_Manager.getInstance().setFrame(frame);
    // 初期画面としてホームへ遷移
    SF_Manager.getInstance().changeScreen(new SF_Home(frame));
}
}