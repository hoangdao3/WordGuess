package org.example.wordgame.server;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 31457;
    private static AtomicBoolean loginSuccess = new AtomicBoolean(false);
    private static AtomicBoolean joinRoomSuccess = new AtomicBoolean(false);

    private enum MenuState {
        MAIN,
        ROOM,
        GAME
    }

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to the server. Type commands to interact.");

            startServerResponseThread(in);

            String currentUsername = null;
            MenuState currentState = MenuState.MAIN;

            while (true) {
                switch (currentState) {
                    case MAIN:
                        currentState = handleMainMenu(scanner, out);
                        break;
                    case ROOM:
                        currentState = handleRoomMenu(scanner, out);
                        break;
                    case GAME:
                        currentState = handleGameMenu(scanner, out);
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void handleServerResponse(String response) {
        String[] parts = response.split(" ", 2);
        String status = parts[0];
        String message = parts.length > 1 ? parts[1] : "";

        if (status.endsWith("_SUCCESS")) {
            switch (status) {
                case "LOGIN_SUCCESS":
                    loginSuccess.set(true);
                    System.out.println("Login successful!");
                    break;
                case "REGISTER_SUCCESS":
                    System.out.println("Registration successful! " + message);
                    break;
                case "LOGOUT_SUCCESS":
                    System.out.println("Logout successful!");
                    break;
                case "CHANGE_PASSWORD_SUCCESS":
                    System.out.println("Password changed successfully!");
                    break;
                case "CREATE_ROOM_SUCCESS":
                    System.out.println("Room created successfully! " + message);
                    break;
                case "JOIN_ROOM_SUCCESS":
                    System.out.println("Successfully joined room! " + message);
                    joinRoomSuccess.set(true);
                    break;
                case "LEAVE_ROOM_SUCCESS":
                    System.out.println("Successfully left room! " + message);
                    break;
                case "LIST_ROOM_SUCCESS":
                    System.out.println("Room list: " + message);
                    break;
                case "SEND_HINT_SUCCESS":
                    System.out.println("Hint sent successfully!");
                    break;
                case "SEND_WORD_SUCCESS":
                    System.out.println("Answer sent successfully!");
                    break;
                default:
                    System.out.println(message);
            }
        } else if (status.endsWith("_FAILURE")) {
            if (status.equals("LOGIN_FAILURE")) {
                loginSuccess.set(false);
            } else if (status.equals("JOIN_ROOM_FAILURE")) {
                joinRoomSuccess.set(false);
            }
            System.out.println("Error: " + message);
        } else {
            System.out.println(response);
        }
    }

    private static MenuState handleMainMenu(Scanner scanner, PrintWriter out) {
        sendMainMenu();
        int choice = getValidIntInput(scanner);

        switch (choice) {
            case 1:
                System.out.print("Enter username: ");
                String username = scanner.nextLine();
                System.out.print("Enter password: ");
                String password = scanner.nextLine();
                loginSuccess.set(false);
                out.println("LOGIN " + username + " " + password);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return loginSuccess.get() ? MenuState.ROOM : MenuState.MAIN;

            case 2:
                System.out.print("Enter username: ");
                String registerUsername = scanner.nextLine();
                System.out.print("Enter password: ");
                String registerPassword = scanner.nextLine();
                out.println("REGISTER " + registerUsername + " " + registerPassword);

                return MenuState.MAIN;

            case 3:
                handleChangePassword(scanner, out);
                
                return MenuState.MAIN;

//            case 4:
//                System.out.println("Exiting...");
//                out.println("exit");
//                System.exit(0);
//                return MenuState.MAIN;

            default:
                System.out.println("Invalid choice.");
                return MenuState.MAIN;
        }
    }

    private static void startServerResponseThread(BufferedReader in) {
        new Thread(() -> {
            String serverResponse;
            try {
                while ((serverResponse = in.readLine()) != null) {
                    handleServerResponse(serverResponse);
                }
            } catch (IOException e) {
                System.out.println("Error reading server response: " + e.getMessage());
            }
        }).start();
    }

    private static MenuState handleRoomMenu(Scanner scanner, PrintWriter out) {
        sendRoomMenu();
        int choice = getValidIntInput(scanner);

        switch (choice) {
            case 1:
                out.println("LIST_ROOM");
                return MenuState.ROOM;

            case 2:
                System.out.print("Enter room name: ");
                String roomName = scanner.nextLine();
                out.println("CREATE_ROOM " + roomName);
                return MenuState.ROOM;

            case 3:
                System.out.print("Enter room name: ");
                String joinRoomName = scanner.nextLine();
                joinRoomSuccess.set(false);
                out.println("JOIN_ROOM " + joinRoomName);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                return joinRoomSuccess.get() ? MenuState.GAME : MenuState.ROOM;

            case 4:
                out.println("LOGOUT");
                return MenuState.MAIN;

            default:
                System.out.println("Invalid choice.");
                return MenuState.ROOM;
        }
    }

    private static MenuState handleGameMenu(Scanner scanner, PrintWriter out) {
        sendGameMenu();
        int choice = getValidIntInput(scanner);

        switch (choice) {
            case 1:
                System.out.print("Enter your hint: ");
                String hint = scanner.nextLine();
                out.println("SEND_HINT " + hint);
                return MenuState.GAME;

            case 2:
                System.out.print("Enter your guess: ");
                String guess = scanner.nextLine();
                out.println("GUESS_WORD " + guess);
                return MenuState.GAME;

            case 3:
                out.println("MY_POINTS");
                return MenuState.GAME;

            case 4:
                out.println("LEAVE_ROOM");
                return MenuState.ROOM;

            default:
                System.out.println("Invalid choice.");
                return MenuState.GAME;
        }
    }

    private static void handleChangePassword(Scanner scanner, PrintWriter out) {
        try {
            System.out.print("Enter username: ");
            String username = scanner.nextLine().trim();
            System.out.print("Enter current password: ");
            String oldPassword = scanner.nextLine().trim();
            System.out.print("Enter new password: ");
            String newPassword = scanner.nextLine().trim();

            String changePasswordCommand = String.format("CHANGE_PASSWORD %s %s %s",
                    username, oldPassword, newPassword);
            out.println(changePasswordCommand);
        } catch (Exception e) {
            System.out.println("Error processing change password: " + e.getMessage());
        }
    }

    private static void sendMainMenu() {
        System.out.println("\nMain Menu:");
        System.out.println("1. Login");
        System.out.println("2. Register");
        System.out.println("3. Change Password");
//        System.out.println("4. Exit");
        System.out.println("--------------------------");
    }

    private static void sendRoomMenu() {
        System.out.println("\nRoom Menu:");
        System.out.println("1. List Rooms");
        System.out.println("2. Create Room");
        System.out.println("3. Join Room");
        System.out.println("4. Logout");
        System.out.println("--------------------------");
    }

    private static void sendGameMenu() {
        System.out.println("\nGame Menu:");
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