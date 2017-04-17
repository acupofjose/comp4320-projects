import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main class that parses command line arguments, initializing a Server or Client
 * based on command line arguments. Please read the included README for all options
 *
 * @author GROUP 10 - Joseph Schultz (jjs0021), Cameron Schaerfl (crs0051), Erich Wu (ejw0013)
 * @version 3-21-2017
 */

public class Main {


    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 10036;
    private static final double DEFAULT_GREMLIN = 0.0;
    private static final String DEFAULT_REQUEST_FILE = "TestFile.html";
    private static final String DEFAULT_OUT_FILE = "reassembled.html";

    /**
     * Parse args and initialize a client or server based on command line arguments
     * Please read the README file for all options
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        Map<String, List<String>> params = parseCommandArgs(args);
        if (params.get("run").get(0).toLowerCase().equals("server")) {
            Server udpServer = initServer(params);
            udpServer.listen();
        } else if (params.get("run").get(0).toLowerCase().equals("client")) {
            Client udpClient = initClient(params);
            udpClient.get();
        } else {
            System.out.println("Flags improperly set please use -run client or -run server");
        }
    }

    /**
     * Initialize a client object
     *
     * @param params params parsed by the parseCommandArgs method
     * @return Constructed client based on command line arguments
     */
    public static Client initClient(Map<String, List<String>> params) {
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;
        double gremlin = DEFAULT_GREMLIN;
        String requestFile = DEFAULT_REQUEST_FILE;
        String outFile = DEFAULT_OUT_FILE;
        if (params.containsKey("port")) {
            port = Integer.parseInt(params.get("port").get(0));
        }
        if (params.containsKey("host")) {
            host = params.get("host").get(0);
        }
        if (params.containsKey("gremlin")) {
            gremlin = Double.parseDouble(params.get("gremlin").get(0));
        }
        if (params.containsKey("rfile")) {
            requestFile = params.get("rfile").get(0);
        }
        if (params.containsKey("ofile")) {
            outFile = params.get("ofile").get(0);
        }
        System.out.println("Program running in CLIENT mode with port: "
                + port + " and host: " + host + " and gremlin chance: " + gremlin);

        return new Client(port, host, gremlin, requestFile, outFile);
    }

    /**
     * Initialize a server object
     *
     * @param params params parsed by the parseCommandArgs method
     * @return Constructed server based on command line arguments
     */
    public static Server initServer(Map<String, List<String>> params) {
        int port = DEFAULT_PORT;
        String host = DEFAULT_HOST;
        if (params.containsKey("port")) {
            port = Integer.parseInt(params.get("port").get(0));
        }
        if (params.containsKey("host")) {
            host = params.get("host").get(0);
        }
        System.out.println("Program running in SERVER mode with port: "
                + port + " and host: " + host);

        return new Server(port, host);
    }

    /**
     * Parse command line arguments into a map of usable parameters
     *
     * @param args the command line arguments
     * @return Map of options to values i.e. -run server = {[run={server}]}
     */
    private static Map<String, List<String>> parseCommandArgs(String[] args) {
        Map<String, List<String>> out = new HashMap<String, List<String>>();
        List<String> options = null;
        for (final String a : args) {
            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    return out;
                }
                options = new ArrayList<String>();
                out.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage. Use '-run server' or '-run client'");
                return out;
            }
        }
        return out;
    }

}
