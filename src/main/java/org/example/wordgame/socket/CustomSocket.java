//package org.example.wordgame.socket;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//class CustomSocket {
//    private static final int AF_INET = 2;
//    private static final int SOCK_STREAM = 1;
//    private final int socketFd;
//    private boolean isClosed = false;
//
//    public CustomSocket() throws IOException {
//        this.socketFd = socket(AF_INET, SOCK_STREAM, 0);
//        if (socketFd < 0) {
//            throw new IOException("Failed to create socket");
//        }
//    }
//
//    public void connect(String address, int port) throws IOException {
//        // Native method to connect socket
//        int result = nativeConnect(socketFd, address, port);
//        if (result < 0) {
//            throw new IOException("Connection failed");
//        }
//    }
//
//    public InputStream getInputStream() throws IOException {
//        return new CustomSocketInputStream(socketFd);
//    }
//
//    public OutputStream getOutputStream() throws IOException {
//        return new CustomSocketOutputStream(socketFd);
//    }
//
//    public void close() throws IOException {
//        if (!isClosed) {
//            nativeClose(socketFd);
//            isClosed = true;
//        }
//    }
//
//    private native int socket(int domain, int type, int protocol);
//    private native int nativeConnect(int fd, String address, int port);
//    private native int nativeClose(int fd);
//}
//
