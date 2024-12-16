package org.example.wordgame.server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    private static final String SERVER_IP = "127.0.0.1";
    private static final int SERVER_PORT = 12345;

    private enum ClientState {
        MAIN_MENU, ROOM_MENU, GAME_MENU
    }

    private static ClientState state = ClientState.MAIN_MENU; // Start with Main Menu

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
                        System.out.println("Server response: " + serverResponse);

                        if (serverResponse.contains("Login successful.")) {
                            state = ClientState.ROOM_MENU;
                            out.println("LIST_ROOMS");
                        } else if (serverResponse.contains("JOIN_ROOM_SUCCESS")) {
                            state = ClientState.GAME_MENU;
                        } else if (serverResponse.contains("LOGOUT_SUCCESS")) {
                            state = ClientState.MAIN_MENU;
                        } else if (serverResponse.contains("LEFT_ROOM")) {
                            state = ClientState.ROOM_MENU;
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error reading server response: " + e.getMessage());
                }
            }).start();

            while (true) {
                displayMenu();
                System.out.println(state);
                int choice = getValidIntInput(scanner); // Use the method to get valid input

                switch (state) {
                    case MAIN_MENU:
                        handleMainMenu(choice, out, scanner);
                        break;

                    case ROOM_MENU:
                        handleRoomMenu(choice, out, scanner);
                        break;

                    case GAME_MENU:
                        handleGameMenu(choice, out, scanner);
                        break;
                }
            }

        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
        }
    }

    private static void displayMenu() {
        switch (state) {
            case MAIN_MENU:
                sendMainMenu();
                break;
            case ROOM_MENU:
                sendRoomMenu();
                break;
            case GAME_MENU:
                sendGameMenu();
                break;
        }
    }

    private static void sendMainMenu() {
        System.out.println("Menu chính:");
        System.out.println("1. Đăng nhập");
        System.out.println("2. Đăng ký");
        System.out.println("3. Đổi mật khẩu");
        System.out.println("4. Thoát");
    }

    private static void sendRoomMenu() {
        System.out.println("Menu phòng:");
        System.out.println("1. Xem danh sách phòng");
        System.out.println("2. Tạo phòng");
        System.out.println("3. Tham gia phòng");
        System.out.println("4. Rời phòng");
        System.out.println("5. Đăng xuất");
    }

    private static void sendGameMenu() {
        System.out.println("Menu game:");
        System.out.println("1. Gửi gợi ý");
        System.out.println("2. Gửi từ đoán");
        System.out.println("3. Gửi hình ảnh gợi ý");
        System.out.println("4. Rời phòng");
        System.out.println("5. Đăng xuất");
    }

    private static void handleMainMenu(int choice, PrintWriter out, Scanner scanner) {
        switch (choice) {
            case 1: // Login
                System.out.print("Nhập username: ");
                String loginUsername = scanner.nextLine();
                System.out.print("Nhập password: ");
                String loginPassword = scanner.nextLine();
                out.println("LOGIN " + loginUsername + " " + loginPassword);
                break;

            case 2: // Register
                System.out.print("Nhập username: ");
                String registerUsername = scanner.nextLine();
                System.out.print("Nhập password: ");
                String registerPassword = scanner.nextLine();
                out.println("REGISTER " + registerUsername + " " + registerPassword);
                break;

            case 3: // Change Password
                System.out.print("Nhập username: ");
                String changePasswordUsername = scanner.nextLine();
                System.out.print("Nhập mật khẩu cũ: ");
                String oldPassword = scanner.nextLine();
                System.out.print("Nhập mật khẩu mới: ");
                String newPassword = scanner.nextLine();
                out.println("CHANGE_PASSWORD " + changePasswordUsername + " " + oldPassword + " " + newPassword);
                break;

            case 4: // Exit
                System.out.println("Đang thoát...");
                out.println("exit");
                System.exit(0);
                break;

            default:
                System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private static void handleRoomMenu(int choice, PrintWriter out, Scanner scanner) {
        switch (choice) {
            case 1:
                out.println("LIST_ROOMS");
                break;

            case 2:
                System.out.print("Nhập tên phòng: ");
                String roomName = scanner.nextLine();
                out.println("CREATE_ROOM " + roomName);
                break;

            case 3:
                System.out.print("Nhập ID phòng: ");
                String roomId = scanner.nextLine();
                out.println("JOIN_ROOM " + roomId);
                break;

            case 4:
                out.println("LEAVE_ROOM");
                break;

            case 5:
                out.println("LOGOUT");
                break;

            default:
                System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    private static void handleGameMenu(int choice, PrintWriter out, Scanner scanner) {
        switch (choice) {
            case 1:
                System.out.print("Nhập gợi ý: ");
                String hint = scanner.nextLine();
                out.println("SEND_HINT " + hint);
                break;

            case 2:
                System.out.print("Nhập từ đoán: ");
                String guess = scanner.nextLine();
                out.println("SEND_GUESS " + guess);
                break;

            case 3:
                System.out.print("Nhập URL hình ảnh gợi ý: ");
                String imageUrl = scanner.nextLine();
                out.println("SEND_IMAGE " + imageUrl);
                break;

            case 4:
                out.println("LEAVE_ROOM");
                break;

            case 5:
                out.println("LOGOUT");
                break;

            default:
                System.out.println("Lựa chọn không hợp lệ.");
        }
    }

    // Method to ensure valid integer input
    private static int getValidIntInput(Scanner scanner) {
        while (true) {
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Vui lòng nhập một số hợp lệ.");
            }
        }
    }
}
