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
    private User currentGuesser;
    private User currentHinter;
    private String currentWord;
    private Timer timer;
    private static final int END_TIMES = GameConstants.END_TIMES;
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
        sendMessageToAll("END_GAME_SUCCESS Time's up! The game has ended.");
        User winner = null;
        int highestScore = Integer.MIN_VALUE;

        for (User  player : players) {
            if (player.getScore() > highestScore) {
                highestScore = player.getScore();
                winner = player;
            }
        }

        if (winner != null) {
            sendMessageToAll("END_GAME_SUCCESS The winner is " + winner.getUsername() + " with a score of " + highestScore + "!");
        } else {
            sendMessageToAll("No winner this time!");
        }

        resetGame();
    }

    public User selectNextHinter(User currentHinter) {
        int currentIndex = players.indexOf(currentHinter);
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
        startTimer();
    }

    private void startTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                endGame();
            }
        }, END_TIMES * 1000); // seconds to milliseconds
    }


    private void resetGame() {
        gameStarted = false;
        currentWord = null;
        if (timer != null) {
            timer.cancel();
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
                System.out.println("Sent: " + message);
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
        int nextIndex = (currentIndex + 1) % players.size();
        return players.get(nextIndex);
    }
    public User selectNewHinter() {
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