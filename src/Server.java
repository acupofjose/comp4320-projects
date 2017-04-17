import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Server class that responds to a request from a client
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 3-21-2017
 */
public class Server {

	/**
	 * Our packet UDP header is always there - it is 8 bytes.
	 * So our data payload size will be 128 - 8 - CHECKSUM_SIZE - SEQUENCE_SIZE bytes.
	 * |          |            |              | 
	 * | CHECKSUM | SEQUENCE # | DATA PAYLOAD |
	 * |__________|____________|______________|
	 *
	 *			   PACKET_SIZE
	 */

	private static final int PACKET_SIZE = 128;
	private static final int CHECKSUM_SIZE = 2;
	private static final int SEQUENCE_SIZE = 2;
	private static final int DATA_OFFSET = 4;
	private static final int CHECKSUM_OFFSET = 0;
	private static final int SEQUENCE_OFFSET = 2;
	private static int data_size = PACKET_SIZE - CHECKSUM_SIZE - SEQUENCE_SIZE;
	private DatagramSocket serverSocket;
	private int serverPort;
	private InetAddress host;
	private byte[] receivedData, sendData;
	private DatagramPacket sendPacket, receivePacket;

	public Server(int port, String host) {
		try {
			receivedData = new byte[PACKET_SIZE];
			sendData = new byte[PACKET_SIZE];
			this.host = InetAddress.getByName(host);
			InetSocketAddress address = new InetSocketAddress(host, port);
			serverSocket = new DatagramSocket(address);
			System.out.println("Bound socket at " + serverSocket.getLocalSocketAddress());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(500);
		}
	}

	public void listen() {
		while (true) {
			try {
				DatagramPacket packet = new DatagramPacket(receivedData, receivedData.length);
				serverSocket.receive(packet);
				String data = new String(packet.getData());
				InetAddress clientHost = packet.getAddress();
				int clientPort = packet.getPort();
				InetSocketAddress clientAddress = new InetSocketAddress(clientHost, clientPort);
				System.out.println("--Received from Client: \n" + data);
				String[] split = data.split(" ");
				if(split.length > 2 && split[0].equals("GET")) {
					if(sendHttpResponse(split[1], clientAddress)) {
						serve(split[1], clientAddress);
					}
				}
			}
			 catch (IOException e) {
				e.printStackTrace();
				System.exit(500);
			}
		}
	}

	public void serve(String filename, InetSocketAddress clientAddress) {
		File file = new File(filename);
		try {
			//Set up file input and read file
			FileInputStream fileInput = new FileInputStream(file);
			int bytesRead;
			byte[] buffer = new byte[data_size];
			short sequence = 0;
			short checksum;
			sendData = new byte[PACKET_SIZE];
			while ((bytesRead = fileInput.read(buffer)) != -1) {
				checksum = checksum(buffer, bytesRead);
				sequence++;
				if (bytesRead != data_size) {
					sendData = new byte[bytesRead + DATA_OFFSET];
				}
				shortToByte(checksum, sendData, CHECKSUM_OFFSET);
				shortToByte(sequence, sendData, SEQUENCE_OFFSET);
				System.arraycopy(buffer, 0, sendData, DATA_OFFSET, bytesRead);
				sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress);
				String sendString = new String(Arrays.copyOfRange(sendPacket.getData(), DATA_OFFSET, sendPacket.getLength() + DATA_OFFSET));
				System.out.println("--Serving packet #" + sequence + " with checksum: " + checksum + " with datasize: " + sendPacket.getLength() + " data payload:\n" + sendString);
				serverSocket.send(sendPacket);
			}
			byte[] terminator = {'\u0000'};
			sendPacket = new DatagramPacket(terminator, terminator.length, clientAddress);
			serverSocket.send(sendPacket);
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = null;
			System.out.print("Served Data: ");
			while ((line = br.readLine()) != null) {
				System.out.println(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(500);
		}
	}

	private static short checksum(byte[] arr, int dataLength) {
		short checksum = 0;
		for(int i = 0; i < dataLength; i ++) {
			checksum += arr[i];
		}
		return checksum;
	}

	public boolean sendHttpResponse(String filename, InetSocketAddress clientAddress) {
		File file = new File(filename);
		if(file.exists()) {
			StringBuilder httpResponse = new StringBuilder("HTTP/1.0 200 Document Follows\r\n");
			httpResponse.append("Content-Type: text/plain\r\n");
			httpResponse.append("Content-Length: ");
			httpResponse.append(file.length());
			httpResponse.append("\r\n");
			httpResponse.append("\r\n");
			byte[] requestBytes = httpResponse.toString().getBytes();
	    	try {
				sendPacket = new DatagramPacket(requestBytes, requestBytes.length, clientAddress);
				System.out.println("--Sending HTTP response: \n" + httpResponse);
	    		serverSocket.send(sendPacket);
	    	} catch (IOException e) {
	    		e.printStackTrace();
	    		System.exit(500);
	    	}
		} else {
			System.out.println("Client requested non existent file - exiting");
			System.exit(404);
		}
		return true;
	}

	/**
	 * Converts a short to a byte and writes into the parameter array
	 * based on the offset value.
	 */
	private static void shortToByte(short value, byte[] arr, int offset) {
		arr[offset + 1] = (byte)(value & 0xff);
		arr[offset] = (byte)((value >> 8) & 0xff);
	}

}
