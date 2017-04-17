import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Server class that responds to a request from a client
 *
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 4-17-2017
 */
public class Server {

    /**
     * Our packet UDP header is always there - it is 8 bytes.
     * So our data payload size will be 512 - 8 - CHECKSUM_SIZE - SEQUENCE_SIZE bytes.
     * |          |            |              |
     * | CHECKSUM | SEQUENCE # | DATA PAYLOAD |
     * |__________|____________|______________|
     * PACKET_SIZE
     */

    /**
     * @TODO Process incoming ACK or NAK packet from within GoBackNThread
     * @TODO Resend all packets based on last NAK
     */
    private static final int PACKET_SIZE = 512;
    private static final int CHECKSUM_SIZE = 2;
    private static final int SEQUENCE_SIZE = 2;
    private static final int DATA_OFFSET = 4;
    private static final int DATA_SIZE = PACKET_SIZE - CHECKSUM_SIZE - SEQUENCE_SIZE;
    private static final int CHECKSUM_OFFSET = 0;
    private static final int SEQUENCE_OFFSET = 2;
    private static final int SEQUENCE_MODULO = 64;
    private static final int WINDOW_SIZE = 32;
    private static final int PACKET_TIMEOUT = 20; //ms
    private static final byte[] TERMINATOR = {'\u0000'};


    private DatagramSocket serverSocket;
    private int serverPort;
    private byte[] receivedData, sendData;
    private DatagramPacket receivePacket;
    private ArrayList<GoBackNThread> threads = new ArrayList<>();

    public Server(int port, String host) {
        try {
            receivedData = new byte[PACKET_SIZE];
            sendData = new byte[PACKET_SIZE];
            InetAddress host1 = InetAddress.getByName(host);
            InetSocketAddress address = new InetSocketAddress(host, port);
            serverSocket = new DatagramSocket(address);
            System.out.println(String.format("Bound socket at %s", serverSocket.getLocalSocketAddress()));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(500);
        }
    }

    void listen() {
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
                serverSocket.receive(packet);
                String data = new String(packet.getData());
                InetAddress clientHost = packet.getAddress();
                int clientPort = packet.getPort();
                InetSocketAddress clientAddress = new InetSocketAddress(clientHost, clientPort);
                GoBackNThread thread = createClientThread(clientAddress);
                System.out.println(String.format("--Received from Client: \n%s", data));
                String[] split = data.split(" ");
                if (split.length > 2 && split[0].equals("GET")) {
                    thread.sendHttpResponse(split[1]);
                    thread.serve(split[1]);
                }
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(500);
            }
        }
    }

    private GoBackNThread createClientThread(InetSocketAddress address) {
        for (GoBackNThread thread : threads) {
            if (thread.clientAddress == address) return thread;
        }
        GoBackNThread thread = new GoBackNThread(address);
        threads.add(thread);
        return thread;
    }

    private static short checksum(byte[] arr, int dataLength) {
        short checksum = 0;
        for (int i = 0; i < dataLength; i++) {
            checksum += arr[i];
        }
        return checksum;
    }


    /**
     * Converts a short to a byte and writes into the parameter array
     * based on the offset value.
     */
    private static void shortToByte(short value, byte[] arr, int offset) {
        arr[offset + 1] = (byte) (value & 0xff);
        arr[offset] = (byte) ((value >> 8) & 0xff);
    }

    /**
     * Encapsulated interactions with a single client (based on address)
     * Each thread instance contains a single interaction with a single client.
     * An interaction consists of:
     * - Queueing packets
     * - Serving packets
     * - Awaiting ACK or NAK
     * - Re-serving packets (if applicable)
     * - Destroying thread
     */
    private class GoBackNThread extends Thread {
        private ArrayList<Packet> queue = new ArrayList<>();
        private Date[] timestamp = new Date[WINDOW_SIZE];
        private Packet[] window = new Packet[WINDOW_SIZE];
        private boolean hasCompletedTransmission = false;
        private boolean isRunning = true;
        private InetSocketAddress clientAddress;
        private DatagramPacket sendPacket;

        GoBackNThread(InetSocketAddress clientAddress) {
            this.clientAddress = clientAddress;
        }

        void enqueue(byte[] data) {
            int nextSequence = queue.size() > 0 ? (queue.get(queue.size() - 1).sequence + 1) : 0;
            short sequence = (short) (nextSequence % SEQUENCE_MODULO);
            queue.add(new Packet(sequence, data));
        }

        public void run() {
            System.out.println(String.format("Creating client thread for %s:%s", clientAddress.getAddress(), clientAddress.getPort()));
            while (isRunning) {
                if (queue.size() > 0) {
                    for (Packet packet : queue) {
                        if (!packet.inTransit) {
                            send(packet); // DO NOT DEQUEUE PACKET UNTIL ACK RECEIVED
                            window[packet.sequence] = packet;
                        }
                    }
                }
                //Once completed sending and receiving all ACKs and NAKs
                if (hasCompletedTransmission) {
                    threads.remove(this);
                    isRunning = false;
                }
            }
        }

        private void send(Packet packet) {
            try {
                packet.sent = new Date().getTime();
                sendPacket = new DatagramPacket(packet.compiled, packet.compiled.length, clientAddress);
                System.out.println("--Sending HTTP response: \n" + new String(packet.buffer));
                serverSocket.send(sendPacket);
                packet.inTransit = true;
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(500);
            }
        }

        private boolean sendHttpResponse(String filename) {
            File file = new File(filename);
            if (file.exists()) {
                String httpResponse = "HTTP/1.0 200 Document Follows\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " +
                        file.length() +
                        "\r\n\r\n";
                byte[] response = httpResponse.getBytes();
                enqueue(response);
            } else {
                System.out.println("Client requested non existent file - exiting");
                System.exit(404);
            }
            return true;
        }

        private void serve(String filename) {
            File file = new File(filename);
            try {
                //Set up file input and read file
                FileInputStream fileInput = new FileInputStream(file);
                int bytesRead;
                byte[] buffer = new byte[DATA_SIZE];

                sendData = new byte[PACKET_SIZE];
                while ((fileInput.read(buffer)) != -1) {
                    enqueue(buffer);
                }
                enqueue(TERMINATOR);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(500);
            }
        }
    }

    /**
     * An instance of a single packet to be sent.
     */
    private class Packet {
        short sequence;
        short checksum;
        byte[] compiled;
        byte[] buffer;
        long created;
        long sent;
        boolean inTransit = false;
        boolean isCompleted = false;

        Packet(short sequence, byte[] data) {
            this.created = new Date().getTime();
            this.checksum = checksum(data, data.length);
            this.buffer = new byte[PACKET_SIZE];
            sequence++;
            shortToByte(checksum, buffer, CHECKSUM_OFFSET);
            shortToByte(sequence, buffer, SEQUENCE_OFFSET);
            System.arraycopy(data, 0, buffer, DATA_OFFSET, data.length);
            this.compiled = data;
        }
    }
}
