package org.example.wordgame.server;

import org.example.wordgame.models.Room;
import org.example.wordgame.models.User;
import org.example.wordgame.utils.DatabaseConnectionPool;
import org.example.wordgame.constant.GameConstants;

import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static final int PORT = 31457;
    private static final List<Room> rooms = new ArrayList<>();
    private static final Map<String, User> loggedInUsers = new ConcurrentHashMap<>();
    private static final Map<User, ClientHandler> clientHandlers = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected");
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private User currentUser ;
        private Room currentRoom;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                    processCommand(message);
                }
            } catch (IOException e) {
                System.err.println("Connection error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try {
                if (currentUser  != null) {
                    loggedInUsers.remove(currentUser .getUsername());
                    if (currentRoom != null) {
                        currentRoom.removePlayer(currentUser );
                        currentRoom.removeClient(socket);
                    }
                }
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }

        private void processCommand(String message) {
            String[] command = message.split(" ", 4);
            if (command.length < 1) {
                sendResponse("Invalid command format.");
                return;
            }

            String commandType = command[0].toUpperCase();

            try {
                switch (commandType) {
                    case "REGISTER":
                        handleRegister(command);
                        break;
                    case "LOGIN":
                        handleLogin(command);
                        break;
                    case "LOGOUT":
                        handleLogout();
                        break;
                    case "CHANGE_PASSWORD":
                        handleChangePassword(command);
                        break;
                    case "LIST_ROOM":
                        handleListRooms();
                        break;
                    case "CREATE_ROOM":
                        handleCreateRoom(command);
                        break;
                    case "JOIN_ROOM":
                        handleJoinRoom(command);
                        break;
                    case "LEAVE_ROOM":
                        handleLeaveRoom();
                        break;
                    case "SEND_HINT":
                        handleSendHint(command);
                        break;
                    case "GUESS_WORD":
                        handleGuessWord(command);
                        break;
                    case "MY_POINTS":
                        handleCheckScore();
                        break;
                    default:
                        sendResponse("Unknown command: " + commandType);
                }
            } catch (Exception e) {
                sendResponse("Error processing command: " + e.getMessage());
            }
        }

        private void handleChangePassword(String[] command) {
            if (command.length != 4) {
                sendResponse("Invalid CHANGE_PASSWORD command. Use: CHANGE_PASSWORD <username> <old_password> <new_password>");
                return;
            }

            String username = command[1];
            String oldPassword = command[2];
            String newPassword = command[3];

            if (oldPassword.equals(newPassword)) {
                sendResponse("CHANGE_PASSWORD_FAILURE New password cannot be the same as the old password.");
                return;
            }

            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                PreparedStatement checkStmt = conn.prepareStatement(
                        "SELECT * FROM users WHERE username = ? AND password = ?"
                );
                checkStmt.setString(1, username);
                checkStmt.setString(2, oldPassword);

                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next()) {
                    sendResponse("CHANGE_PASSWORD_FAILURE Incorrect old password.");
                    return;
                }

                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET password = ? WHERE username = ?"
                );
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, username);

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    sendResponse("CHANGE_PASSWORD_SUCCESS");
                } else {
                    sendResponse("CHANGE_PASSWORD_FAILURE Failed to change password.");
                }
            } catch (SQLException e) {
                sendResponse("CHANGE_PASSWORD_FAILURE Error changing password: " + e.getMessage());
            }
        }


        private void handleRegister(String[] command) {
            if (command.length != 3) {
                sendResponse("REGISTER_FAILURE Please enter complete information");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                String checkUsernameQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkUsernameQuery)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getInt(1) > 0) {
                            sendResponse("REGISTER_FAILURE Username already exists");
                            return;
                        }
                    }
                }

                String insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);
                    stmt.executeUpdate();
                    sendResponse("REGISTER_SUCCESS " + username);
                }

            } catch (SQLException e) {
                sendResponse("REGISTER_FAILURE: " + e.getMessage());
            }
        }


        private void handleLogin(String[] command) {
            if (command.length != 3) {
                sendResponse("LOGIN_FAILURE Invalid command. Use: LOGIN <username> <password>");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                String checkUsernameQuery = "SELECT * FROM users WHERE username = ?";
                try (PreparedStatement stmt = conn.prepareStatement(checkUsernameQuery)) {
                    stmt.setString(1, username);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String storedPassword = rs.getString("password");
                            if (storedPassword.equals(password)) {
                                currentUser = new User(username);
                                loggedInUsers.put(username, currentUser);
                                clientHandlers.put(currentUser, this);
                                sendResponse("LOGIN_SUCCESS");
                            } else {
                                sendResponse("LOGIN_FAILURE Incorrect password");
                            }
                        } else {
                            sendResponse("LOGIN_FAILURE Username does not exist");
                        }
                    }
                }
            } catch (SQLException e) {
                sendResponse("LOGIN_FAILURE Login failed: " + e.getMessage());
            }
        }


        private void handleLogout() {
            if (currentUser != null) {
                loggedInUsers.remove(currentUser.getUsername());

                if (currentRoom != null) {
                    currentRoom.removePlayer(currentUser);
                    currentRoom.removeClient(socket);
                }

                currentUser = null;
                currentRoom = null;

                sendResponse("LOGOUT_SUCCESS");
            } else {
                sendResponse("LOGIN_FAILURE User not logged in");
            }
        }

        private void handleListRooms() {
            if (rooms.isEmpty()) {
                sendResponse("LIST_ROOM_SUCCESS  No rooms available");
            } else {
                StringBuilder roomList = new StringBuilder("LIST_ROOM_SUCCESS Available Rooms: ");
                for (Room room : rooms) {
                    roomList.append(room.getRoomName()).append(" ");
                }
                sendResponse(roomList.toString());
            }
        }

        private void handleCreateRoom(String[] command) {
            if (currentUser == null) {
                sendResponse("CREATE_ROOM_FAILURE You need to be logged in to create a room.");
                return;
            }

            if (rooms.size() >= GameConstants.MAX_ROOMS) {
                sendResponse("CREATE_ROOM_FAILURE Maximum room limit reached.");
                return;
            }

            if (command.length != 2) {
                sendResponse("CREATE_ROOM_FAILURE Invalid CREATE_ROOM command. ");
                return;
            }

            String roomName = command[1];

            for (Room room : rooms) {
                if (room.getRoomName().equalsIgnoreCase(roomName)) {
                    sendResponse("CREATE_ROOM_FAILURE Room '" + roomName + "' already exists.");
                    return;
                }
            }

            Room newRoom = new Room(roomName);
            rooms.add(newRoom);
            sendResponse("CREATE_ROOM_SUCCESS Room '" + roomName + "' created successfully.");
        }


        private void handleJoinRoom(String[] command) {
            if (currentUser == null) {
                sendResponse("JOIN_ROOM_FAILURE You need to be logged in to join a room.");
                return;
            }

            if (command.length != 2) {
                sendResponse("JOIN_ROOM_FAILURE Invalid JOIN_ROOM command.");
                return;
            }

            String roomName = command[1];
            currentUser.setScore(GameConstants.INIT_PLAYER_POINTS);
            for (Room room : rooms) {

                if (room.getRoomName().equalsIgnoreCase(roomName)) {
                    if (room.getCurrentPlayers() >= GameConstants.MAX_MEMBERS) {
                        sendResponse("JOIN_ROOM_FAILURE Room is full. You cannot join.");
                        return;
                    }
                    currentRoom = room;
                    currentRoom.addUser(currentUser);
                    currentRoom.addClient(socket);
                    sendResponse("JOIN_ROOM_SUCCESS Joined room: " + roomName);

                    if (currentRoom.getCurrentPlayers() >= GameConstants.START_MEMBERS && !currentRoom.isGameStarted()) {
                        currentRoom.startGame();
                        User firstPlayer = currentRoom.getFirstPlayer();
                        currentRoom.setCurrentHinter(firstPlayer);

                        Random random = new Random();
                        String randomWord = GameConstants.GUESS_WORDS.get(random.nextInt(GameConstants.GUESS_WORDS.size()));
                        currentRoom.setCurrentWord(randomWord);

                        currentRoom.sendMessageToAll("START GAME! " + firstPlayer.getUsername() + " is the hinter.");
                        ClientHandler firstPlayerHandler = clientHandlers.get(firstPlayer);
                        if (firstPlayerHandler != null) {
                            firstPlayerHandler.sendResponse("NOTIFICATION_ROLE_SUCCESS You are the hinter! The word to guess is: " + randomWord);
                        }

                        for (User player : currentRoom.getPlayers()) {
                            if (!player.equals(firstPlayer)) {
                                ClientHandler playerHandler = clientHandlers.get(player);
                                if (playerHandler != null) {
                                    playerHandler.sendResponse("NOTIFICATION_ROLE_SUCCESS You are a guesser. Try to guess the word!");
                                }
                            }
                        }
                    }
                    return;
                }
            }
            sendResponse("JOIN_ROOM_FAILURE Room '" + roomName + "' not found");
        }


        private void handleLeaveRoom() {
            if (currentUser == null) {
                sendResponse("LEAVE_ROOM_FAILURE You need to be logged in to leave a room.");
                return;
            }

            if (currentRoom != null) {
                currentRoom.removePlayer(currentUser);
                currentRoom.removeClient(socket);

                if (currentRoom.getCurrentPlayers() == 0) {
                    rooms.remove(currentRoom);

                    sendResponse("LEAVE_ROOM_SUCCESS The room has been removed as it is empty.");
                } else {
                    sendResponse("LEAVE_ROOM_SUCCESS Left the current room");
                }

                currentRoom = null;
            } else {
                sendResponse("LEAVE_ROOM_FAILURE You are not in any room");
            }
        }


        private void handleSendHint(String[] command) {
            if (currentUser == null) {
                sendResponse("SEND_HINT_FAILURE You need to be logged in to send a hint.");
                return;
            }

            if (currentRoom == null) {
                sendResponse("SEND_HINT_FAILURE You need to join a room to send a hint.");
                return;
            }

            if (!currentUser.equals(currentRoom.getCurrentHinter())) {
                sendResponse("SEND_HINT_FAILURE You cannot send hints while you are the guesser.");
                return;
            }

            String hint = String.join(" ", Arrays.copyOfRange(command, 1, command.length));

            currentRoom.sendMessageToAll("SEND_HINT_TO_USERS_SUCCESS " + currentUser.getUsername() + " sends a hint: " + hint);
            sendResponse("SEND_HINT_SUCCESS");
        }
        private void handleGuessWord(String[] command) {
            if (command.length < 2) {
                sendResponse("GUESS_WORD_FAILURE Invalid GUESS_WORD command.");
                return;
            }

            String guessedWord = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
            String correctWord = currentRoom.getCurrentWord();

            if (currentUser.equals(currentRoom.getCurrentHinter())) {
                sendResponse("GUESS_WORD_FAILURE You cannot guess the word while you are the hinter.");
                return;
            }

            if (currentUser.isHasGuessed()) {
                sendResponse("GUESS_WORD_FAILURE You have already guessed this round. Please wait for the next round.");
                return;
            }

            boolean isCorrectGuess = guessedWord.equalsIgnoreCase(correctWord);
            currentUser.setHasGuessed(true);
            currentUser.setGuessedCorrectly(isCorrectGuess);

            if (isCorrectGuess) {
                currentRoom.sendMessageToAll("GUESS_RESULT_SUCCESS " + currentUser.getUsername() + " guessed correctly!");
            } else {
                currentRoom.sendMessageToAll("GUESS_RESULT_SUCCESS " + currentUser.getUsername() + " guessed incorrectly!");
                currentUser.addScore(-1);
            }

            int totalPlayers = currentRoom.getPlayers().size() - 1;
            int guessedPlayers = 0;
            for (User player : currentRoom.getPlayers()) {
                if (player.isHasGuessed() && !player.equals(currentRoom.getCurrentHinter())) {
                    guessedPlayers++;
                }
            }

            int correctPoints = correctWord.length();
            if (guessedPlayers >= totalPlayers) {
                currentRoom.sendMessageToAll("ALL_GUESSED_SUCCESS All players have made their guesses!");
                int correctGuesses = 0;
                for (User player : currentRoom.getPlayers()) {
                    if (player.isHasGuessed() && !player.equals(currentRoom.getCurrentHinter())) {
                        if (player.isGuessedCorrectly()) {
                            correctGuesses++;
                            player.addScore(correctPoints);
                            currentRoom.sendMessageToAll("OVER_ROUND_SUCCESS " + player.getUsername() + " earns " + correctPoints + " points for correct guess!");
                        }
                    }
                }

                User hinter = currentRoom.getCurrentHinter();
                int hinterPoints = correctPoints * correctGuesses;
                hinter.addScore(hinterPoints);
                currentRoom.sendMessageToAll("OVER_ROUND_SUCCESS " + hinter.getUsername() + " (hinter) earns " + hinterPoints + " points!");

                for (User player : currentRoom.getPlayers()) {
                    player.setHasGuessed(false);
                    player.setGuessedCorrectly(false);
                }

                User oldHinter = currentRoom.getCurrentHinter();
                User newHinter = currentRoom.selectNextHinter(oldHinter);
                currentRoom.setCurrentHinter(newHinter);

                Random random = new Random();
                String randomWord = GameConstants.GUESS_WORDS.get(random.nextInt(GameConstants.GUESS_WORDS.size()));
                currentRoom.setCurrentWord(randomWord);

                ClientHandler newHinterHandler = clientHandlers.get(newHinter);
                if (newHinterHandler != null) {
                    newHinterHandler.sendResponse("NOTIFICATION_ROLE_SUCCESS You are the new hinter! The word to guess is: " + randomWord);
                }
                for (User player : currentRoom.getPlayers()) {
                    if (!player.equals(newHinter)) {
                        ClientHandler playerHandler = clientHandlers.get(player);
                        if (playerHandler != null) {
                            playerHandler.sendResponse("NEW_ROUND_SUCCESS New round! " + newHinter.getUsername() + " is the new hinter. Get ready to guess!");
                        }
                    }
                }
            }
        }

        private void handleCheckScore() {
            sendResponse("MY_POINTS_SUCCESS Your points are: " + currentUser  .getScore());
        }

        private void sendResponse(String response ) {
            out.println(response);
            System.out.println("Sent: " + response);
        }
    }
}