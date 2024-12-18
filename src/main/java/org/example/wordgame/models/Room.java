package org.example.wordgame.models;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Room {
    private String roomName;
    private List<User> players;
    private boolean gameStarted;
    private List<Socket> clientSockets;
    private User currentGuesser; // Track the current guesser
    private User currentHinter; // Track the current hinter
    private String currentWord; // Track the current word to be guessed
    private Timer timer; // Timer for the game
    private static final int END_TIMES = 60; // Duration of the game in seconds
    public User getFirstPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }
    public Room(String roomName) {
        this.roomName = roomName;
        this.players = new ArrayList<>();
        this.gameStarted = false;
        this.clientSockets = new ArrayList<>();
    }

    public String getRoomName() {
        return roomName;
    }

    public void addUser (User user) {
        players.add(user);
    }

    public void removePlayer(User user) {
        players.remove(user);
    }

    public int getCurrentPlayers() {
        return players.size();
    }

    public List<User> getPlayers() {
        return players;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void startGame() {
        gameStarted = true;
        startTimer(); // Start the timer when the game starts
    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame();
            }
        }, END_TIMES * 1000); // Convert seconds to milliseconds
    }

    private void endGame() {
        // Notify all players of the end of the game
        sendMessageToAll("Time's up! The game has ended.");

        // Determine the winner
        User winner = null;
        int highestScore = Integer.MIN_VALUE;

        for (User  player : players) {
            if (player.getScore() > highestScore) {
                highestScore = player.getScore();
                winner = player;
            }
        }

        if (winner != null) {
            sendMessageToAll("The winner is " + winner.getUsername() + " with a score of " + highestScore + "!");
        } else {
            sendMessageToAll("No winner this time!");
        }

        // Clean up the room or reset the game as needed
        resetGame();
    }

    private void resetGame() {
        // Reset game state if needed
        gameStarted = false;
        currentWord = null;
        // Additional reset logic can be added here
        if (timer != null) {
            timer.cancel(); // Cancel the timer
        }
    }

    public void addClient(Socket socket) {
        clientSockets.add(socket);
    }

    public void removeClient(Socket socket) {
        clientSockets.remove(socket);
    }

    public List<Socket> getClientSockets() {
        return clientSockets;
    }

    public void sendMessageToAll(String message) {
        for (Socket socket : clientSockets) {
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                System.err.println("Error sending message to client: " + e.getMessage());
            }
        }
    }

    public User getCurrentGuesser() {
        return currentGuesser;
    }

    public void setCurrentGuesser(User currentGuesser) {
        this.currentGuesser = currentGuesser;
    }

    public User getCurrentHinter() {
        return currentHinter;
    }

    public void setCurrentHinter(User currentHinter) {
        this.currentHinter = currentHinter;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public void setCurrentWord(String currentWord) {
        this.currentWord = currentWord;
    }
}