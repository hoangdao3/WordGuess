//package org.example.wordgame.socket;
//
//import java.io.IOException;
//
//class CustomServerSocket {
//    private final CustomSocket socket;
//
//    public CustomServerSocket() throws IOException {
//        this.socket = new CustomSocket();
//    }
//
//    public void bind(int port, int backlog) throws IOException {
//        int result = nativeBind(socket.getFd(), port);
//        if (result < 0) {
//            throw new IOException("Bind failed");
//        }
//
//        result = nativeListen(socket.getFd(), backlog);
//        if (result < 0) {
//            throw new IOException("Listen failed");
//        }
//    }
//
//    public CustomSocket accept() throws IOException {
//        int clientFd = nativeAccept(socket.getFd());
//        if (clientFd < 0) {
//            throw new IOException("Accept failed");
//        }
//        return new CustomSocket(clientFd);
//    }
//
//    private native int nativeBind(int fd, int port);
//    private native int nativeListen(int fd, int backlog);
//    private native int nativeAccept(int fd);
//}