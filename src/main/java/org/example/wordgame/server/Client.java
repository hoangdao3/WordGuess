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
            boolean inRoom = false; // Track if the user is in a room

            while (true) {
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
                        handleRoomMenu(out, in, scanner, currentUsername, inRoom); // Pass the username and room status
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

    private static void handleRoomMenu(PrintWriter out, BufferedReader in, Scanner scanner, String currentUsername, boolean inRoom) {
        while (true) {
            sendRoomMenu();
            int choice = getValidIntInput(scanner);

            switch (choice) {
                case 1: // List Rooms
                    if (inRoom) {
                        System.out.println("You must leave the room before listing rooms.");
                    } else {
                        out.println("LIST_ROOM");
                    }
                    break;

                case 2: // Create Room
                    if (inRoom) {
                        System.out.println("You must leave the room before creating a new room.");
                    } else {
                        System.out.print("Enter room name: ");
                        String roomName = scanner.nextLine();
                        out.println("CREATE_ROOM " + currentUsername + " " + roomName); // Use stored username
                    }
                    break;

                case 3: // Join Room
                    System.out.print("Enter room name: ");
                    String joinRoomName = scanner.nextLine();
                    out.println("JOIN_ROOM " + currentUsername + " " + joinRoomName); // Use stored username
                    inRoom = true; // Set inRoom to true after joining a room
                    sendGameMenu(); // Immediately display the game menu
                    break;

                case 4: // Leave Room
                    out.println("LEAVE_ROOM");
                    inRoom = false; // Set inRoom to false after leaving a room
                    break;

                case 5: // Logout
                    out.println("LOGOUT");
                    return; // Exit the room menu and return to the main menu

                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    private static void sendRoomMenu() {
        System.out.println("Room Menu:");
        System.out.println("1. List Rooms");
        System.out.println("2. Create Room");
        System.out.println("3. Join Room");
        System.out.println("4. Leave Room");
        System.out.println("5. Logout");
        System.out.println("--------------------------");
    }

    private static void sendGameMenu() {
        System.out.println("Game Menu:");
        System.out.println("1. Start Game");
        System.out.println("2. View Scores");
        System.out.println("3. Exit Game");
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