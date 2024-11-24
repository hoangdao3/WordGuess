//package org.example.wordgame.socket;
//
//import java.io.IOException;
//import java.io.InputStream;
//
//class CustomBufferedReader {
//    private final InputStream in;
//    private final byte[] buffer = new byte[8192];
//    private int pos = 0;
//    private int limit = 0;
//
//    public CustomBufferedReader(InputStream in) {
//        this.in = in;
//    }
//
//    public String readLine() throws IOException {
//        StringBuilder line = new StringBuilder();
//        int c;
//        while ((c = read()) != -1) {
//            if (c == '\n') {
//                break;
//            }
//            if (c != '\r') {
//                line.append((char)c);
//            }
//        }
//        return line.length() > 0 ? line.toString() : null;
//    }
//
//    private int read() throws IOException {
//        if (pos >= limit) {
//            limit = in.read(buffer);
//            pos = 0;
//            if (limit == -1) {
//                return -1;
//            }
//        }
//        return buffer[pos++] & 0xff;
//    }
//}