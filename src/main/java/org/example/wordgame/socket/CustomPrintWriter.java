//package org.example.wordgame.socket;
//
//import java.io.IOException;
//import java.io.OutputStream;
//
//class CustomPrintWriter {
//    private final OutputStream out;
//    private final byte[] lineBreak = System.lineSeparator().getBytes();
//
//    public CustomPrintWriter(OutputStream out) {
//        this.out = out;
//    }
//
//    public void println(String str) throws IOException {
//        byte[] bytes = str.getBytes();
//        out.write(bytes);
//        out.write(lineBreak);
//        out.flush();
//    }
//}