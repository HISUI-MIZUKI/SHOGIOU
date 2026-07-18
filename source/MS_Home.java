package source;

import java.awt.Font;
import java.awt.GridBagConstraints; // java.awt パッケージからインポート（修正済）
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * タイトル画面（ホーム画面）を表示するクラス。
 */
public class MS_Home extends JPanel {
    private static MS_Home instance;

    private MS_Home() {
        // コンポーネントを中央寄せにするため GridBagLayout を採用
        setLayout(new GridBagLayout());

        // タイトルラベルの設定
        JLabel titleLabel = new JLabel("将☆戯☆王");
        titleLabel.setFont(new Font("Serif", Font.BOLD, 36));
        
        // 対戦開始ボタンの設定
        JButton playButton = new JButton("オフライン対戦 (2P)");
        playButton.setFont(new Font("SansSerif", Font.PLAIN, 18));

        // ② ボタン押下時、マネージャ経由でオフライン対戦画面へ遷移
        playButton.addActionListener(e -> {
            Singleton_Manager.getInstance().changeScreen("OFFLINE_2P");
        });

        // GridBagConstraints を使って上下中央に綺麗に配置
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 10); // 上下左右に10ピクセルの余白
        add(titleLabel, gbc);

        gbc.gridy = 1; // 縦位置を1つ下に下げる
        add(playButton, gbc);
    }

    public static synchronized MS_Home getInstance() {
        if (instance == null) {
            instance = new MS_Home();
        }
        return instance;
    }
}
