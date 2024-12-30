package org.example.wordgame.socket;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class SocketServer {
    private final int port;
    private ServerSocket serverSocket;
    private final ExecutorService pool;
    private volatile boolean running = true;

    public SocketServer(int port) {
        this.port = port;
        this.pool = Executors.newCachedThreadPool();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            stop();
        }
    }

    public void stop() {
        running = false;
        pool.shutdown();
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                setupStreams();
                handleClient();
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void setupStreams() throws IOException {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        private void handleClient() throws IOException {
            String message;
            while ((message = in.readLine()) != null) {
                // Handle incoming messages here
                System.out.println("Received: " + message);
                out.println("Server received: " + message);
            }
        }

        private void cleanup() {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                System.err.println("Error cleaning up: " + e.getMessage());
            }
        }
    }
}
