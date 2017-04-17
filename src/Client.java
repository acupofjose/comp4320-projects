
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Client class that requests a file based on parameters and attempts to grab and reassemble
 * the requested file.
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 3-21-2017
 */

public class Client {

	/**
	 * Our packet UDP header is always there - it is 8 bytes.
	 * So our data payload size will be 128 - 8 - CHECKSUM_SIZE - SEQUENCE_SIZE bytes.
	 * |          |            |              | 
	 * | CHECKSUM | SEQUENCE # | DATA PAYLOAD |
	 * |__________|____________|______________|
	 *
	 *			   PACKET_SIZE
	 */

	/**
	 * PACKET_SIZE: total size in bytes of our packet payload
	 * UDP_HEADER_SIZE: total size in bytes of the UDP header
	 * CHECKSUM_SIZE: total size in bytes of the checksum of our data
	 * SEQUENCE_SIZE: total size in bytes of the sequence number of a packet
	 * CHECKSUM_OFFSET: offset of the checksum from the start of the data array
	 */
	private static final int PACKET_SIZE = 128;
	private static final int CHECKSUM_SIZE = 2;
	private static final int SEQUENCE_SIZE = 2;
	private static final int DATA_OFFSET = 4;
	private static final int CHECKSUM_OFFSET = 0;
	private static final int SEQUENCE_OFFSET = 2;
	private static int data_size = PACKET_SIZE - CHECKSUM_SIZE - SEQUENCE_SIZE;

	private DatagramSocket clientSocket;
	private InetSocketAddress serverAddress;
	private byte[] sendData, receiveData;
	private DatagramPacket sendPacket, receivePacket;
	private Gremlin gremlin;
	private int checksumErrors, packetsReceived;
	private byte[] reassembledData;
	private String requestFile;
	private String outFile;

	/**
	 * Initialize client and set up server address.
	 * @param port The port of the server socket
	 * @param host The host of the server socket
	 * @param gremlin_chance The chance of Gremlin corruption
	 * @param requestFile The file to request from the server
     * @param outFile The file to write reassembled data
     */
    public Client(int port, String host, double gremlin_chance, String requestFile, String outFile) {
        try {
            clientSocket = new DatagramSocket();
            serverAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            sendData = new byte[PACKET_SIZE];
            receiveData = new byte[PACKET_SIZE];
            gremlin = new Gremlin(gremlin_chance);
            checksumErrors = 0;
            packetsReceived = 0;
			this.requestFile = requestFile;
			this.outFile = outFile;
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the request file from the server
     */

    public void get() {
    	sendGetRequest();
    	int fileLength = receiveGetResponse();
    	reassembledData = new byte[fileLength];
		receiveData = new byte[PACKET_SIZE];
		receivePacket = new DatagramPacket(receiveData, receiveData.length);
    	try {
    		while (true) {
    			clientSocket.receive(receivePacket);
    			if(receivePacket.getData()[0] == (byte) '\u0000') break;
    			short checksum = parseShort(receivePacket.getData(), CHECKSUM_OFFSET);
				short sequence = parseShort(receivePacket.getData(), SEQUENCE_OFFSET);
				receivePacket = gremlin.touchPacket(receivePacket);
				byte[] dataBytes = Arrays.copyOfRange(receivePacket.getData(), DATA_OFFSET, receivePacket.getLength());
    			String stringData = new String(dataBytes);
    			System.out.println("--Received packet #" + sequence + " with checksum: " + checksum + " with datasize: " + receivePacket.getLength() + " data payload:\n" + stringData);
    			boolean checksumVerified = verifyChecksum(dataBytes, checksum);
    			packetsReceived++;
    			if (checksumVerified) {
    				System.out.println("--Packet #" + sequence + " Verified ");
    			} else {
    				System.out.println("--Packet #" + sequence + " CHECKSUM ERROR");
    				checksumErrors++;
    			}
    			reassemble(dataBytes, sequence, dataBytes.length);
    			receivePacket = new DatagramPacket(receiveData, receiveData.length);
    		}
    		System.out.println("Reassembled data: " + new String(reassembledData));
			try{
                PrintWriter out = new PrintWriter( outFile );
                out.print( new String(reassembledData) );
                out.flush();
			} catch (IOException e) {
                e.printStackTrace();
                System.exit(400);
            }
    		System.out.println((100 * (double) checksumErrors / (double) packetsReceived) + "% checksum error rate");
    	} catch (IOException e) {
    		e.printStackTrace();
    		System.exit(400);
    	}
    }

    /**
     * Send the get request of a file to the server.
     */
    private void sendGetRequest() {
    	String request = "GET " + requestFile + " HTTP/1.0";
    	byte[] requestBytes = request.getBytes();
    	try {
            sendPacket = new DatagramPacket(requestBytes, requestBytes.length, serverAddress);
            System.out.println("--Sending HTTP GET request to server: \n" + request);
    		clientSocket.send(sendPacket);
    	} catch (IOException e) {
    		e.printStackTrace();
    		System.exit(400);
    	}
    }

    /**
     * Receive the HTTP response from the server.
     * @return return the size of the file being served
     */
    private int receiveGetResponse() {
    	byte[] responseBytes = new byte[1024];
    	receivePacket = new DatagramPacket(responseBytes, responseBytes.length);
    	try {
    		clientSocket.receive(receivePacket);
    		String response = new String(receivePacket.getData());
    		System.out.println("--Received HTTP response from Server: \n" + response);
    		String[] split = response.split(" ");
    		int fileSize = Integer.parseInt(split[split.length - 1].trim());
    		return fileSize;
    	} catch (IOException e) {
    		e.printStackTrace();
    		System.exit(400);
    	}
    	return -1;
    }

	/**
	 * Reassemble data by copying data into correct offset of reassembled data array
	 * @param data the data from the packet
	 * @param sequence the sequence number of the packet - used to calculate final offset
	 * @param dataLength the length of the data in the packet
     */
    private void reassemble(byte[] data, short sequence, int dataLength) {
    	sequence--;
    	System.out.println("Attempting to reassemble: " + Arrays.toString(data));
    	System.out.println("Attempting to reassemble at: " + (sequence * (PACKET_SIZE - DATA_OFFSET)) + " to " + (sequence * (PACKET_SIZE - DATA_OFFSET) + dataLength));
		if(sequence * (PACKET_SIZE - DATA_OFFSET) + dataLength > reassembledData.length) {
			dataLength = reassembledData.length  - sequence * (PACKET_SIZE - DATA_OFFSET);
		}
		System.arraycopy(data, 0, reassembledData, sequence * (PACKET_SIZE - DATA_OFFSET), dataLength);
    }

    /**
     * Parse a short from two adjacent indices of a byte array
     * @param arr byte array where we will parse a short from two adjacent indices
     * @param offset int representing the offset of the starting index to parse the short
     * @return the resultant short of parsing
     */
    private short parseShort(byte[] arr, int offset) {
    	return (short)( ((arr[offset]&0xFF)<<8) | (arr[offset + 1]&0xFF) );
    }

    /**
     * Calculate a checksum from a given byte array and return if it matches 
     * the parameter checksum
     * @param data byte array from which the checksum will be calculated
     * @param checksum short representing the checksum to verify
     * @return boolean whether the calculated checksum matches the parameter checksum
     */
    private boolean verifyChecksum(byte[] data, short checksum) {
    	short calc_checksum = 0;
    	for (int i = 0; i < data.length; i++) {
    		calc_checksum += data[i];
    	}
    	return calc_checksum == checksum;
    }
}


