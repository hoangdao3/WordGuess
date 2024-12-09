package org.example.wordgame.models;

import org.example.wordgame.constant.GameConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Room {
    private int id;
    private String roomName;
    private List<String> players; // List of players in the room
    private String guesser; // The player who is guessing the word
    private String wordToGuess; // The word to guess

    public Room(int id, String roomName) {
        this.id = id;
        this.roomName = roomName;
        this.players = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public String getRoomName() {
        return roomName;
    }

    public void addUser (String username) {
        if (!players.contains(username)) {
            players.add(username);
            // If this is the first player, set them as the guesser
            if (players.size() == 1) {
                guesser = username;
                wordToGuess = getRandomWord(); // Assign a random word to the guesser
            }
        }
    }

    public void removeUser (String username) {
        players.remove(username);
        // If the guesser leaves, reset the guesser and word
        if (username.equals(guesser)) {
            guesser = null;
            wordToGuess = null;
        }
    }

    public boolean containsUser (String username) {
        return players.contains(username);
    }

    public List<String> getPlayers() {
        return players;
    }

    public String getGuesser() {
        return guesser;
    }

    public String getWordToGuess() {
        return wordToGuess;
    }

    private String getRandomWord() {
        Random random = new Random();
        return GameConstants.GUESS_WORDS.get(random.nextInt(GameConstants.GUESS_WORDS.size()));
    }
}