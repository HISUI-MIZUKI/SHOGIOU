package source;

import java.util.ArrayList;
import java.util.List;

/**
 * デッキのマスター情報を管理するクラス。
 * 企画書に基づき、飛車デッキと角デッキの初期構成を提供します。
 */
public class DB_Deck {

    /**
     * 指定されたデッキタイプに応じた初期駒リスト（18枚）を返します。
     * @param deckType "HI" (飛車デッキ) または "KA" (角デッキ)
     * @return 駒ID（文字列）のリスト
     */
    public static List<String> getInitialDeck(String deckType) {
        List<String> deck = new ArrayList<>();

        // 共通の駒を追加
        // 金x2, 銀x2, 桂馬x2, 香車x2
        for (int i = 0; i < 2; i++) {
            deck.add("KI"); // 金
            deck.add("GI"); // 銀
            deck.add("KE"); // 桂
            deck.add("KY"); // 香
        }
        // 歩兵x9
        for (int i = 0; i < 9; i++) {
            deck.add("FU");
        }

        // エース駒の追加
        switch (deckType) {
            case "HI":
                // 飛車デッキのエース駒は飛車
                deck.add("HI");
                break;
            case "KA":
                // 角デッキのエース駒は角行
                deck.add("KA");
                break;
            default:
                throw new IllegalArgumentException("不正なデッキタイプ: " + deckType);
        }

        return deck;
    }
}