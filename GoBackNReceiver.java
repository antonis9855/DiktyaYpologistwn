import java.io.*;
import java.net.*;
import java.util.*;

public class GoBackNReceiver {
    public static boolean receiveFile(String sellerIp, int sellerPort, String objectId, String username) {
        Random random = new Random();
        ByteArrayOutputStream fileBytes = new ByteArrayOutputStream();
        int expected = 0;
        int total = -1;
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(7000);
            send(socket, InetAddress.getByName(sellerIp), sellerPort, "GET|" + objectId);
            while (true) {
                byte[] buf = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String[] parts = msg.split("\\|", 4);
                if (parts[0].equals("FIN")) {
                    if (total >= 0 && expected >= total) break;
                    continue;
                }
                if (!parts[0].equals("DATA") || parts.length != 4) continue;
                if (random.nextDouble() < 0.20) {
                    System.out.println("[" + username + "] GBN dropped packet");
                    continue;
                }
                int seq = Integer.parseInt(parts[1]);
                total = Integer.parseInt(parts[2]);
                if (seq == expected) {
                    byte[] data = Base64.getDecoder().decode(parts[3]);
                    fileBytes.write(data);
                    expected++;
                    System.out.println("[" + username + "] GBN received seq=" + seq);
                } else {
                    System.out.println("[" + username + "] GBN discarded out of order seq=" + seq);
                }
                if (random.nextDouble() < 0.80) {
                    send(socket, packet.getAddress(), packet.getPort(), "ACK|" + expected);
                    System.out.println("[" + username + "] GBN ACK " + expected);
                } else {
                    System.out.println("[" + username + "] GBN ACK lost");
                }
                if (total >= 0 && expected >= total) {
                    send(socket, packet.getAddress(), packet.getPort(), "ACK|" + expected);
                    break;
                }
            }
            File dir = new File("shared_directory_" + username);
            dir.mkdir();
            try (FileOutputStream out = new FileOutputStream(new File(dir, objectId + ".txt"))) {
                fileBytes.writeTo(out);
            }
            System.out.println("[" + username + "] GBN saved " + objectId);
            return true;
        } catch (Exception e) {
            System.out.println("[" + username + "] GBN receive failed: " + e.getMessage());
            return false;
        }
    }

    static void send(DatagramSocket socket, InetAddress address, int port, String text) throws IOException {
        byte[] bytes = text.getBytes();
        socket.send(new DatagramPacket(bytes, bytes.length, address, port));
    }
}
