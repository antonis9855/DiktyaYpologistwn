import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class GoBackNSender implements Runnable {
    DatagramSocket socket;
    InetAddress address;
    int port;
    String objectId;
    int windowSize = 3;

    public GoBackNSender(DatagramSocket socket, InetAddress address, int port, String objectId) {
        this.socket = socket;
        this.address = address;
        this.port = port;
        this.objectId = objectId;
    }

    public void run() {
        String username = Peer.myUsername;
        if (username == null) return;
        File file = new File("shared_directory_" + username, objectId + ".txt");
        if (!file.exists()) return;
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            int total = (data.length + 63) / 64;
            int base = 0;
            int next = 0;
            socket.setSoTimeout(2000);
            while (base < total) {
                while (next < total && next < base + windowSize) {
                    sendPacket(data, next, total);
                    next++;
                }
                try {
                    byte[] buf = new byte[128];
                    DatagramPacket ackPacket = new DatagramPacket(buf, buf.length);
                    socket.receive(ackPacket);
                    String ack = new String(ackPacket.getData(), 0, ackPacket.getLength());
                    String[] parts = ack.split("\\|");
                    if (parts.length == 2 && parts[0].equals("ACK")) {
                        int ackNum = Integer.parseInt(parts[1]);
                        if (ackNum > base) {
                            base = ackNum;
                            System.out.println("[" + username + "] GBN ACK " + ackNum);
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("[" + username + "] GBN timeout, retransmit from " + base);
                    next = base;
                }
            }
            sendText("FIN|" + total);
            file.delete();
            System.out.println("[" + username + "] GBN sent and deleted " + objectId);
        } catch (Exception e) {
            System.out.println("[" + username + "] GBN send failed: " + e.getMessage());
        } finally {
            try { socket.setSoTimeout(0); } catch (Exception e) {}
        }
    }

    void sendPacket(byte[] data, int seq, int total) throws IOException {
        int start = seq * 64;
        int len = Math.min(64, data.length - start);
        byte[] part = Arrays.copyOfRange(data, start, start + len);
        String payload = Base64.getEncoder().encodeToString(part);
        sendText("DATA|" + seq + "|" + total + "|" + payload);
        System.out.println("[" + Peer.myUsername + "] GBN send seq=" + seq);
    }

    void sendText(String text) throws IOException {
        byte[] bytes = text.getBytes();
        DatagramPacket p = new DatagramPacket(bytes, bytes.length, address, port);
        socket.send(p);
    }
}
