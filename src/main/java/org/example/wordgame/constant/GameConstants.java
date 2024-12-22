package org.example.wordgame.constant;


import java.util.Arrays;
import java.util.List;

public class GameConstants {
    public static final int MAX_ROOMS = 3;
    public static final int INIT_PLAYER_POINTS = 10;
    public static final int INIT_GUESS_POINTS = 10;
    public static final List<String> GUESS_WORDS = Arrays.asList(
            "apple", "banana", "orange",
            "mango", "grape", "pineapple",
            "cherry", "pear", "guava",
            "duck", "dog", "cat",
            "elephant", "tiger", "chicken",
            "horse", "fish", "dragon",
            "vietnam", "france", "usa",
            "japan", "korea", "china",
            "england", "italy", "spain"
    );

    public static final int START_MEMBERS =00;
    public static final int END_TIMES = 300; // seconds

}
