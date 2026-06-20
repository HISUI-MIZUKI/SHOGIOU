package source.Main_ScreenFlow;

import javax.swing.*;

public class SF_Manager {
    private static final SF_Manager instance = new SF_Manager();
    private ScreenFlow currentScreen;
    private JFrame frame;

    private SF_Manager() {}

    public static SF_Manager getInstance() {
        return instance;
    }

    public void setFrame(JFrame frame) {
        this.frame = frame;
    }

    public void changeScreen(ScreenFlow nextScreen) {
        if (currentScreen != null) {
            currentScreen.Final();
        }
        currentScreen = nextScreen;
        // SF_Game_Offlineへ遷移する際、Initが呼ばれ盤面が生成される
        currentScreen.Init();
        currentScreen.Run();
    }
}