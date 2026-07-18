package source;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * ゲームの起動エントリーポイント。
 * メインウィンドウ（JFrame）を生成し、画面管理マネージャを初期化します。
 */
public class main {
    public static void main(String[] args) {
        // SwingのUIスレッド（イベントディスパッチスレッド）で安全に画面を起動
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("将☆戯☆王 - プロトタイプ");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null); // 画面をPCモニターの中央に配置

            // 【シングルトン】画面マネージャを取得し、メインフレームを登録して初期化
            Singleton_Manager manager = Singleton_Manager.getInstance();
            manager.init(frame);

            frame.setVisible(true);

            // ① 起動完了後、最初の画面である「ホーム画面」へ遷移
            manager.changeScreen("HOME");
        });
    }
}
