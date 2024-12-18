package org.example.wordgame.server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server. Type commands to interact.");

            new Thread(() -> {
                String serverResponse;
                try {
                    while ((serverResponse = in.readLine()) != null) {
                        System.out.println(serverResponse);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading server response: " + e.getMessage());
                }
            }).start();

            String currentUsername = null; // Variable to store the logged-in username
            boolean isLogin = false; // Track if the user is logged in
            boolean isJoinRoom = false; // Track if the user is in a room

            while (true) {
                if (!isLogin) {
                    sendMainMenu();
                    int choice = getValidIntInput(scanner);

                    switch (choice) {
                        case 1: // Login
                            System.out.print("Enter username: ");
                            String loginUsername = scanner.nextLine();
                            System.out.print("Enter password: ");
                            String loginPassword = scanner.nextLine();
                            out.println("LOGIN " + loginUsername + " " + loginPassword);
                            currentUsername = loginUsername; // Store the username after successful login
                            isLogin = true; // Set isLogin to true after successful login
                            break;

                        case 2: // Register
                            System.out.print("Enter username: ");
                            String registerUsername = scanner.nextLine();
                            System.out.print("Enter password: ");
                            String registerPassword = scanner.nextLine();
                            out.println("REGISTER " + registerUsername + " " + registerPassword);
                            break;

                        case 3: // Change Password
                            System.out.print("Enter username: ");
                            String changePasswordUsername = scanner.nextLine();
                            System.out.print("Enter old password: ");
                            String oldPassword = scanner.nextLine();
                            System.out.print("Enter new password: ");
                            String newPassword = scanner.nextLine();
                            out.println("CHANGE_PASSWORD " + changePasswordUsername + " " + oldPassword + " " + newPassword);
                            break;

                        case 4: // Exit
                            System.out.println("Exiting...");
                            out.println("exit");
                            System.exit(0);
                            break;

                        default:
                            System.out.println("Invalid choice.");
                    }
                } else {
                    // User is logged in, show room menu
                    handleRoomMenu(out, in, scanner, currentUsername, isJoinRoom);
                }
            }

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void sendMainMenu() {
        System.out.println("Main Menu:");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Change Password");
        System.out.println("4. Exit");
        System.out.println("--------------------------");
    }

    private static void handleRoomMenu(PrintWriter out, BufferedReader in, Scanner scanner, String currentUsername, boolean isJoinRoom) {
        while (true) {
            if (!isJoinRoom) {
                sendRoomMenu();
            } else {
                sendGameMenu();
            }

            int choice = getValidIntInput(scanner);

            if (!isJoinRoom) {
                switch (choice) {
                    case 1: // List Rooms
                        out.println("LIST_ROOM");
                        break;

                    case 2: // Create Room
                        System.out.print("Enter room name: ");
                        String roomName = scanner.nextLine();
                        out.println("CREATE_ROOM " + roomName);
                        break;

                    case 3: // Join Room
                        System.out.print("Enter room name: ");
                        String joinRoomName = scanner.nextLine();
                        out.println("JOIN_ROOM " + joinRoomName); // Use stored username
                        isJoinRoom = true; // Set isJoinRoom to true after joining a room
                        break;

                    case 4: // Logout
                        out.println("LOGOUT");
                        isJoinRoom = false; // Reset room status on logout
                        return; // Exit the room menu and return to the main menu

                    default:
                        System.out.println("Invalid choice.");
                }
            } else {
                switch (choice) {
                    case 1: // Send Hint
                        System.out.print("Enter your hint: ");
                        String hint = scanner.nextLine();
                        out.println("SEND_HINT " + hint);
                        break;

                    case 2: // Guess Word
                        System.out.print("Enter your guess: ");
                        String guess = scanner.nextLine();
                        out.println("GUESS_WORD " + guess);
                        break;

                    case 3: // Check Score
                        out.println("CHECK_SCORE");
                        break;

                    case 4: // Leave Room
                        out.println("LEAVE_ROOM");
                        isJoinRoom = false; // Reset room status when leaving
                        break;

                    default:
                        System.out.println("Invalid choice.");
                }
            }
        }
    }

    private static void sendRoomMenu() {
        System.out.println("Room Menu:");
        System.out.println("1. List Rooms");
        System.out.println("2. Create Room");
        System.out.println("3. Join Room");
        System.out.println("4. Logout");
        System.out.println("--------------------------");
    }

    private static void sendGameMenu() {
        System.out.println("Game Menu:");
        System.out.println("1. Send Hint");
        System.out.println("2. Guess Word");
        System.out.println("3. Check Score");
        System.out.println("4. Leave Room");
        System.out.println("--------------------------");
    }

    private static int getValidIntInput(Scanner scanner) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }
}