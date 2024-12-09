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
//        private void handleSendHint(String[] command) {
//            if (command.length < 2) {
//                sendResponse("Invalid SEND_HINT command.");
//                return;
//            }
//            String hint = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
//            sendResponse("Hint sent: " + hint);
//        }
//
//        private void handleReceiveHintText(String[] command) {
//            if (command.length < 2) {
//                sendResponse("Invalid RECEIVE_HINT_TEXT command.");
//                return;
//            }
//            String hintText = String.join(" ", Arrays.copyOfRange(command, 1, command.length));
//            sendResponse("Received hint text: " + hintText);
//        }
//
//        private void handleReceiveHintImage(String[] command) {
//            if (command.length < 2) {
//                sendResponse("Invalid RECEIVE_HINT_IMAGE command.");
//                return;
//            }
//            String imagePath = command[1];
//            sendResponse("Received hint image from: " + imagePath);
//        }
//
//        private void handleSendWord(String[] command) {
//            if (command.length < 2) {
//                sendResponse("Invalid SEND_WORD command.");
//                return;
//            }
//            String word = command[1];
//            sendResponse("Word sent: " + word);
//        }
//
//        private void handleMyPoints(String username) {
//            if (!loggedInUsers.containsKey(username)) {
//                sendResponse("You must log in first.");
//                return;
//            }
//            int points = currentUser .getPoints();
//            sendResponse("Your points: " + points);
//        }