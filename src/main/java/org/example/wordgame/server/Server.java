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
            String[] command = message.split(" ", 3);
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

        private void handleRegister(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid REGISTER command. Use: REGISTER <username> <password>");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.executeUpdate();
                sendResponse("Registration successful for " + username);
            } catch (SQLException e) {
                sendResponse("Registration failed: " + e.getMessage());
            }
        }

        private void handleLogin(String[] command) {
            if (command.length != 3) {
                sendResponse("Invalid LOGIN command. Use: LOGIN <username> <password>");
                return;
            }

            String username = command[1];
            String password = command[2];

            try (Connection conn = DatabaseConnectionPool.getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        currentUser  = new User(username);
                        loggedInUsers.put(username, currentUser );
                        clientHandlers.put(currentUser , this);
                        sendResponse("Login successful for " + username);
                    } else {
                        sendResponse("Invalid username or password");
                    }
                }
            } catch (SQLException e) {
                sendResponse("Login failed: " + e.getMessage());
            }
        }

        private void handleLogout() {
            if (currentUser  != null) {
                loggedInUsers.remove(currentUser .getUsername());
                if (currentRoom != null) {
                    currentRoom.removePlayer(currentUser );
                    currentRoom.removeClient(socket);
                }
                currentUser  = null;
                currentRoom = null;
                sendResponse("Logout successful");
            } else {
                sendResponse("User  not logged in");
            }
        }

        private void handleListRooms() {
            if (rooms.isEmpty()) {
                sendResponse("No rooms available");
            } else {
                StringBuilder roomList = new StringBuilder("Available Rooms: ");
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
                    currentRoom.addUser (currentUser );
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
                currentRoom = null;
                sendResponse("Left the current room");
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
            if (!currentUser .equals(currentRoom.getCurrentHinter())) {
                sendResponse("You cannot send hints while you are the guesser.");
                return;
            }

            String hint = String.join(" ", Arrays.copyOfRange (command, 1, command.length));
            currentRoom.sendMessageToAll(currentUser .getUsername() + " sends a hint: " + hint);
        }

        private void handleGuessWord(String[] command) {
            if (command.length < 2) {
                sendResponse("Invalid GUESS_WORD command. Use: GUESS_WORD <word>");
                return;
            }

            String guessedWord = command[1];
            String correctWord = currentRoom.getCurrentWord(); // Assume you have a method to get the current word

            // Check if the current user is the guesser
            if (currentUser .equals(currentRoom.getCurrentHinter())) {
                sendResponse("You cannot guess the word while you are the hinter.");
                return;
            }

            // Check if the guessed word is correct
            if (guessedWord.equalsIgnoreCase(correctWord)) {
                currentUser .addScore(guessedWord.length()); // Add points equal to the length of the word
                currentRoom.sendMessageToAll(currentUser .getUsername() + " guessed correctly! Score: " + currentUser .getScore());
            } else {
                currentUser .subtractScore(1); // Subtract 1 point for an incorrect guess
                currentRoom.sendMessageToAll(currentUser .getUsername() + " guessed incorrectly! Score: " + currentUser .getScore());
            }
        }

        private void handleCheckScore() {
            // Logic to retrieve and send the score of the current user
            // For now, we will just send a placeholder message
            sendResponse("Your current score is: " + currentUser .getScore());
        }

        private void sendResponse(String response) {
            out.println(response);
            System.out.println("Sent: " + response);
        }
    }
}