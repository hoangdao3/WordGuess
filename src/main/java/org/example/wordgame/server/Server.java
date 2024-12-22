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
    private static final int PORT = 12345;
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
                    case "CHECK_SCORE":
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

            // Kiểm tra xem mật khẩu cũ và mới có trùng nhau không
            if (oldPassword.equals(newPassword)) {
                sendResponse("CHANGE_PASSWORD_FAILURE New password cannot be the same as the old password.");
                return;
            }

            try (Connection conn = DatabaseConnectionPool.getConnection()) {
                // Kiểm tra tên người dùng và mật khẩu cũ
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

                // Cập nhật mật khẩu mới
                PreparedStatement updateStmt = conn.prepareStatement(
                        "UPDATE users SET password = ? WHERE username = ?"
                );
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, username);

                int rowsAffected = updateStmt.executeUpdate();
                if (rowsAffected > 0) {
                    sendResponse("CHANGE_PASSWORD_SUCCESS Password changed successfully for user: " + username);
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
                // Kiểm tra xem tên người dùng đã tồn tại hay chưa
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

                // Nếu tên người dùng chưa tồn tại, chèn thông tin người dùng mới vào cơ sở dữ liệu
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
                            // Tên người dùng tồn tại, kiểm tra mật khẩu
                            String storedPassword = rs.getString("password");
                            if (storedPassword.equals(password)) {
                                // Mật khẩu đúng, đăng nhập thành công
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
            if (currentUser   != null) {
                loggedInUsers.remove(currentUser  .getUsername());
                if (currentRoom != null) {
                    currentRoom.removePlayer(currentUser  );
                    currentRoom.removeClient(socket);
                }
                currentUser   = null;
                currentRoom = null;
                sendResponse("Logout successful");
            } else {
                sendResponse("User  not logged in");
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
            if (command.length != 2) {
                sendResponse("Invalid CREATE_ROOM command. Use: CREATE_ROOM <room_name>");
                return;
            }

            String roomName = command[1];

            for (Room room : rooms) {
                if (room.getRoomName().equalsIgnoreCase(roomName)) {
                    sendResponse("Room '" + roomName + "' already exists. Please choose a different name.");
                    return;
                }
            }

            Room newRoom = new Room(roomName);
            rooms.add(newRoom);
            sendResponse("Room '" + roomName + "' created successfully.");
        }

        private void handleJoinRoom(String[] command) {
            if (command.length != 2) {
                sendResponse("Invalid JOIN_ROOM command. Use: JOIN_ROOM <room_name>");
                return;
            }

            String roomName = command[1];

            for (Room room : rooms) {
                if (room.getRoomName().equalsIgnoreCase(roomName)) {
                    currentRoom = room;
                    currentRoom.addUser (currentUser  );
                    currentRoom.addClient(socket);
                    sendResponse("Joined room: " + roomName);

                    // Check if the game can start
                    if (currentRoom.getCurrentPlayers() >= GameConstants.START_MEMBERS && !currentRoom.isGameStarted()) {
                        currentRoom.startGame();
                        User firstPlayer = currentRoom.getFirstPlayer();
                        currentRoom.setCurrentHinter(firstPlayer); // Set the first player as the hinter

                        // Randomly select a word from the GUESS_WORDS list
                        Random random = new Random();
                        String randomWord = GameConstants.GUESS_WORDS.get(random.nextInt(GameConstants.GUESS_WORDS.size()));
                        currentRoom.setCurrentWord(randomWord); // Set the current word

                        // Notify all clients in the room
                        currentRoom.sendMessageToAll("The game is starting! " + firstPlayer.getUsername() + " is the hinter. Everyone else is a guesser. The word to guess has been set.");

                        // Notify the first player using the ClientHandler
                        ClientHandler firstPlayerHandler = clientHandlers.get(firstPlayer);
                        if (firstPlayerHandler != null) {
                            firstPlayerHandler.sendResponse("You are the hinter! The word to guess is: " + randomWord);
                        }

                        // Notify other players
                        for (User  player : currentRoom.getPlayers()) {
                            if (!player.equals(firstPlayer)) {
                                ClientHandler playerHandler = clientHandlers.get(player);
                                if (playerHandler != null) {
                                    playerHandler.sendResponse("You are a guesser. Try to guess the word!");
                                }
                            }
                        }
                    }
                    return;
                }
            }
            sendResponse("Room '" + roomName + "' not found");
        }

        private void handleLeaveRoom() {
            if (currentRoom != null) {
                currentRoom.removePlayer(currentUser );
                currentRoom.removeClient(socket);

                // Check if the room is empty
                if (currentRoom.getCurrentPlayers() == 0) {
                    rooms.remove(currentRoom); // Remove the room from the list
                    sendResponse("The room has been removed as it is empty.");
                } else {
                    sendResponse("Left the current room");
                }

                currentRoom = null;
            } else {
                sendResponse("You are not in any room");
            }
        }

        private void handleSendHint(String[] command) {
            if (command.length < 2) {
                sendResponse("Invalid SEND_HINT command. Use: SEND_HINT <hint>");
                return;
            }

            // Check if the current user is the guesser
            if (!currentUser  .equals(currentRoom.getCurrentHinter())) {
                sendResponse("You cannot send hints while you are the guesser.");
                return;
            }

            String hint = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
            currentRoom.sendMessageToAll(currentUser  .getUsername() + " sends a hint: " + hint);
        }

        private void handleGuessWord(String[] command) {
            if (command.length < 2) {
                sendResponse("Invalid GUESS_WORD command. Use: GUESS_WORD <word>");
                return;
            }

            String guessedWord = command[1];
            String correctWord = currentRoom.getCurrentWord();

            // Check if the current user is the hinter
            if (currentUser.equals(currentRoom.getCurrentHinter())) {
                sendResponse("You cannot guess the word while you are the hinter.");
                return;
            }

            // Check if the user has already guessed
            if (currentUser.isHasGuessed()) {
                sendResponse("You have already guessed this round. Please wait for the next round.");
                return;
            }

            // Mark the user as having guessed
            currentUser.setHasGuessed(true);

            // Check if the current user's guess is correct
            boolean isCorrectGuess = guessedWord.equalsIgnoreCase(correctWord);
            if (isCorrectGuess) {
                currentRoom.sendMessageToAll(currentUser.getUsername() + " guessed correctly!");
            } else {
                currentRoom.sendMessageToAll(currentUser.getUsername() + " guessed incorrectly!");
            }

            // Check if all players (excluding the hinter) have guessed
            int totalPlayers = currentRoom.getPlayers().size() - 1; // Exclude the hinter
            int guessedPlayers = 0;
            for (User player : currentRoom.getPlayers()) {
                if (player.isHasGuessed() && !player.equals(currentRoom.getCurrentHinter())) {
                    guessedPlayers++;
                }
            }

            // If all players have guessed
            if (guessedPlayers >= totalPlayers) {
                currentRoom.sendMessageToAll("All players have made their guesses!");

                // Count the number of correct guesses
                int correctGuesses = 0;
                for (User player : currentRoom.getPlayers()) {
                    if (player.isHasGuessed() && !player.equals(currentRoom.getCurrentHinter())) {
                        if (guessedWord.equalsIgnoreCase(correctWord)) {
                            correctGuesses++;
                        }
                    }
                }

                // Award points to players who guessed correctly
                for (User player : currentRoom.getPlayers()) {
                    if (player.isHasGuessed() && !player.equals(currentRoom.getCurrentHinter())) {
                        if (guessedWord.equalsIgnoreCase(correctWord)) {
                            int scoreToAdd = correctGuesses * correctWord.length();
                            player.addScore(scoreToAdd);
                            currentRoom.sendMessageToAll(player.getUsername() + " earns " + scoreToAdd + " points!");
                        }
                    }
                }

                // Reset guess state for all players
                for (User player : currentRoom.getPlayers()) {
                    player.setHasGuessed(false);
                }

                // Select a new hinter
                User oldHinter = currentRoom.getCurrentHinter();
                User newHinter = currentRoom.selectNextHinter(oldHinter);
                currentRoom.setCurrentHinter(newHinter);

                // Select a new random word
                Random random = new Random();
                String randomWord = GameConstants.GUESS_WORDS.get(random.nextInt(GameConstants.GUESS_WORDS.size()));
                currentRoom.setCurrentWord(randomWord);

                // Notify the new hinter
                ClientHandler newHinterHandler = clientHandlers.get(newHinter);
                if (newHinterHandler != null) {
                    newHinterHandler.sendResponse("You are the new hinter! The word to guess is: " + randomWord);
                }

                // Notify other players
                for (User player : currentRoom.getPlayers()) {
                    if (!player.equals(newHinter)) {
                        ClientHandler playerHandler = clientHandlers.get(player);
                        if (playerHandler != null) {
                            playerHandler.sendResponse("New round! " + newHinter.getUsername() + " is the new hinter. Get ready to guess!");
                        }
                    }
                }
            }
        }

        private void handleCheckScore() {
            sendResponse("Your current score is: " + currentUser  .getScore());
        }

        private void sendResponse(String response) {
            out.println(response);
            System.out.println("Sent: " + response);
        }
    }
}