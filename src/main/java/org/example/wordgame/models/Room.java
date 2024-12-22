package org.example.wordgame.models;

import org.example.wordgame.constant.GameConstants;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Room {
    private String roomName;
    private List<User> players;
    private boolean gameStarted;
    private List<Socket> clientSockets;
    private User currentGuesser; // Track the current guesser
    private User currentHinter; // Track the current hinter
    private String currentWord; // Track the current word to be guessed
    private Timer timer; // Timer for the game
    private static final int END_TIMES = GameConstants.END_TIMES; // Duration of the game in seconds
    public User getFirstPlayer() {
        return players.isEmpty() ? null : players.get(0);
    }
    public Room(String roomName) {
        this.roomName = roomName;
        this.players = new ArrayList<>();
        this.gameStarted = false;
        this.clientSockets = new ArrayList<>();
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

    public User selectNextHinter(User currentHinter) {
        int currentIndex = players.indexOf(currentHinter);
        // Lấy người chơi tiếp theo trong danh sách, quay lại đầu nếu đã đến cuối
        return players.get((currentIndex + 1) % players.size());
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
//    private void endGame() {
//        // Notify all players of the end of the game
//        sendMessageToAll("Time's up! The game has ended.");
//
//        // Determine the winner
//        User winner = null;
//        int highestScore = Integer.MIN_VALUE;
//
//        for (User  player : players) {
//            if (player.getScore() > highestScore) {
//                highestScore = player.getScore();
//                winner = player;
//            }
//        }
//
//        if (winner != null) {
//            sendMessageToAll("The winner is " + winner.getUsername() + " with a score of " + highestScore + "!");
//        } else {
//            sendMessageToAll("No winner this time!");
//        }
//
//        // Clean up the room or reset the game as needed
//        resetGame();
//
//        // Check if the room is empty after the game ends
//        if (players.isEmpty()) {
//            // Logic to remove the room can be handled in the ClientHandler or in the Server class
//            // For now, we can just notify that the room is empty
//            sendMessageToAll("The room has been removed as it is empty.");
//        }
//    }
    public boolean allPlayersGuessed() {
        for (User  player : players) {
            if (!player.isHasGuessed()) {
                return false;
            }
        }
        return true;
    }
    public User selectNextGuesser() {
        int currentIndex = players.indexOf(currentGuesser);
        // Get the next player in line, wrap around if at the end of the list
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }
    public User selectNewHinter() {
        // Select a new hinter randomly from the players
        Random random = new Random();
        return players.get(random.nextInt(players.size()));
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