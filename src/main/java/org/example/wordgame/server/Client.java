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

            String command;
            while (true) {
                command = scanner.nextLine();
                out.println(command);

                if (command.equalsIgnoreCase("exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}