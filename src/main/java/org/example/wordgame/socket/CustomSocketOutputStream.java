//package org.example.wordgame.socket;
//
//import java.io.IOException;
//import java.io.OutputStream;
//
//class CustomSocketOutputStream extends OutputStream {
//    private final int socketFd;
//    private final byte[] singleByteBuf = new byte[1];
//
//    public CustomSocketOutputStream(int socketFd) {
//        this.socketFd = socketFd;
//    }
//
//    @Override
//    public void write(int b) throws IOException {
//        singleByteBuf[0] = (byte)b;
//        write(singleByteBuf, 0, 1);
//    }
//
//    @Override
//    public void write(byte[] b, int off, int len) throws IOException {
//        int n = nativeWrite(socketFd, b, off, len);
//        if (n < 0) {
//            throw new IOException("Write failed");
//        }
//    }
//
//    private native int nativeWrite(int fd, byte[] b, int off, int len);
//}