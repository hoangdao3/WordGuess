//package org.example.wordgame.socket;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//class CustomSocketInputStream extends InputStream {
//    private final int socketFd;
//    private final byte[] singleByteBuf = new byte[1];
//
//    public CustomSocketInputStream(int socketFd) {
//        this.socketFd = socketFd;
//    }
//
//    @Override
//    public int read() throws IOException''
//    {
//        int n = read(singleByteBuf, 0, 1);
//        return n == -1 ? -1 : singleByteBuf[0] & 0xff;
//    }
//
//    @Override
//    public int read(byte[] b, int off, int len) throws IOException {
//        return nativeRead(socketFd, b, off, len);
//    }
//
//    private native int nativeRead(int fd, byte[] b, int off, int len);
//}