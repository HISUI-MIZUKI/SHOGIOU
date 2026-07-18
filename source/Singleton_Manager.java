package source;

import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * 各画面（パネル）を一元管理し、画面遷移を制御するシングルトンクラス。
 * CardLayout（紙芝居形式）を利用してパネルを切り替えます。
 */
public class Singleton_Manager {
    private static Singleton_Manager instance;
    
    private JPanel containerPanel; // 画面を重ねて格納するベースパネル
    private CardLayout cardLayout;  // 画面切り替えの制御レイアウト

    // 外部からのインスタンス化を禁止
    private Singleton_Manager() {}

    public static synchronized Singleton_Manager getInstance() {
        if (instance == null) {
            instance = new Singleton_Manager();
        }
        return instance;
    }

    /**
     * マネージャの初期化と各画面の登録を行います。
     * @param frame メインのウィンドウ
     */
    public void init(JFrame frame) {
        this.cardLayout = new CardLayout();
        this.containerPanel = new JPanel(cardLayout);

        // 各画面クラス（すべてシングルトン）をコンテナに登録
        containerPanel.add(MS_Home.getInstance(), "HOME");
        containerPanel.add(MS_Offline2P.getInstance(), "OFFLINE_2P");

        frame.add(containerPanel);
    }

    /**
     * 指定された名前の画面へ遷移します。
     * @param screenName 遷移先の画面識別子 ("HOME" や "OFFLINE_2P")
     */
    public void changeScreen(String screenName) {
        cardLayout.show(containerPanel, screenName);
        
        // 【引き継ぎ時の注意】画面が切り替わったタイミングで初期化処理を走らせる
        if ("OFFLINE_2P".equals(screenName)) {
            MS_Offline2P.getInstance().onScreenOpened();
        }
    }
}
