import java.net.*;

public class UdpPeerListener implements Runnable {
    int port;

    public UdpPeerListener(int port) {
        this.port = port;
    }

    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[PEER] UDP listening on port " + port);
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), 0, packet.getLength());
                String[] parts = msg.split("\\|");
                if (parts.length == 2 && parts[0].equals("GET")) {
                    new GoBackNSender(socket, packet.getAddress(), packet.getPort(), parts[1]).run();
                }
            }
        } catch (Exception e) {
            System.out.println("[PEER] UDP listener stopped.");
        }
    }
}
