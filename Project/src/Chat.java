import javax.json.*;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.Semaphore;

public class Chat {
    /*----- Data members -----*/

    // Client info
    String alias;
    int myPort;

    // Successor info
    String ipSuccessor;
    int portSuccessor;

    // Predecessor info
    String ipPredecessor;
    int portPredecessor;

    // Lock
    Semaphore dataSemaphore;

    /*----- Accessors -----*/

    /**
     * Alias accessor
     * @return the name of the client
     */
    public String getAlias() {
        return this.alias;
    }

    /**
     * Port accessor
     * @return the port of the client
     */
    public int getPort() {
        return this.myPort;
    }

    /**
     * IP successor accessor
     * @return the IP address of the client's successor
     */
    public String getIpSuccessor() {
        return this.ipSuccessor;
    }

    /**
     * Port successor accessor
     * @return the port of the client's successor
     */
    public int getPortSuccessor() {
        return this.portSuccessor;
    }

    /**
     * IP predecessor accessor
     * @return the IP address of the client's predecessor
     */
    public String getIpPredecessor() {
        return this.ipPredecessor;
    }

    /**
     * Port predecessor accessor
     * @return the port of the client's predecessor
     */
    public int getPortPredecessor() {
        return this.portPredecessor;
    }

    /*----- Mutators -----*/

    /**
     * IP successor mutator. This method blocks until it can acquire the lock.
     * @param newIp the new IP address of the client's successor
     */
    public void updateIpSuccessor(String newIp) {
        try {
            dataSemaphore.acquire();
            this.ipSuccessor = newIp;
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            dataSemaphore.release();
        }
    }

    /**
     * Port successor mutator. This method blocks until it can acquire the lock.
     * @param newPort the new port of the client's successor
     */
    public void updatePortSuccessor(int newPort) {
        try {
            dataSemaphore.acquire();
            this.portSuccessor = newPort;
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            dataSemaphore.release();
        }
    }

    /**
     * IP predecessor mutator. This method blocks until it can acquire the lock.
     * @param newIp the new IP address of the client's predecessor
     */
    public void updateIpPredecessor(String newIp) {
        try {
            dataSemaphore.acquire();
            this.ipPredecessor = newIp;
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            dataSemaphore.release();
        }
    }

    /**
     * Port predecessor mutator. This method blocks until it can acquire the lock.
     * @param newPort the new port of the client's predecessor
     */
    public void updatePortPredecessor(int newPort) {
        try {
            dataSemaphore.acquire();
            this.portPredecessor = newPort;
        } catch(InterruptedException e) {
            e.printStackTrace();
        } finally {
            dataSemaphore.release();
        }
    }

    /**
    * Creates a JSON message for JOINs
    * @param alias the desired client to join
    * @param myPort the source port of the client who wishes to join
    * @return Json object containing type and parameters
    */
    public JsonObject createMessage_JOIN(String alias, int myPort) {
        JsonObject joinJson = Json.createObjectBuilder()
            .add("type", "JOIN")
            .add("parameters", Json.createObjectBuilder()
                .add("myAlias", alias)
                .add("myPort", myPort))
            .build();
        return joinJson;
    }

    /**
    * Creates a JSON message for ACCEPTs
    * @param ip the source ip of the client who was accepted
    * @param port the source port of the client was accepted
    * @return Json object containing type and parameters
    */
    public JsonObject createMessage_ACCEPT(String ip, int port) {
        JsonObject acceptJson = Json.createObjectBuilder()
            .add("type", "ACCEPT")
            .add("parameters", Json.createObjectBuilder()
                .add("ipPred", ip)
                .add("portPred", port))
            .build();
        return acceptJson;
    }

    /**
    * Creates a JSON message for NEW SUCCESSORs
    * @param ip the new successor's ip address
    * @param port the new successor's port
    * @return Json object containing type and parameters
    */
    public JsonObject createMessage_NEWSUCCESSOR(String ip, int port) {
        JsonObject newSuccessorJson = Json.createObjectBuilder()
            .add("type", "NEWSUCCESSOR")
            .add("parameters", Json.createObjectBuilder()
                .add("ipSuccessor", ip)
                .add("portSuccessor", port))
            .build();
        return newSuccessorJson;
    }

    /**
    * Creates a JSON message for PUTs
    * @param aliasSender the name of the original client sender
    * @param aliasReceiver the name of the destination client
    * @param message the message sender wants receiver to see
    * @return Json object containing type and parameters
    */
    public JsonObject createMessage_PUT(String aliasSender, String aliasReceiver, String message) {
        JsonObject putJson = Json.createObjectBuilder()
            .add("type", "PUT")
            .add("parameters", Json.createObjectBuilder()
                .add("aliasSender", aliasSender)
                .add("aliasReceiver", aliasReceiver)
                .add("message", message))
            .build();
        return putJson;
    }

    /**
    * Creates a JSON message for LEAVEs
    * @param ip the source ip of the client who is leaving
    * @param port the source port of the client who is leaving
    * @return Json object containing type and parameters
    */
    public JsonObject createMessage_LEAVE(String ip, int port) {
        JsonObject leaveJson = Json.createObjectBuilder()
            .add("type", "LEAVE")
            .add("parameters", Json.createObjectBuilder()
                .add("ipPred", ip)
                .add("portPred", port))
            .build();
        return leaveJson;
    }

    /**
     * Sends a JSON message over a socket connection
     * @param jsonMessage the JSON to send
     * @param ip the IP address to send the message to
     * @param port the port to connect to
     */
    public void sendJson(JsonObject jsonMessage, String ip, int port) {
        try {
            Socket socket = new Socket(ip, port);
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            JsonWriter jsonWriter = Json.createWriter(oos);
            jsonWriter.write(jsonMessage);
            jsonWriter.close();

            socket.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Constructor for the Chat class
    * @param alias the name of the client
    * @param myPort the port that the client exists on.
    */
    public Chat(String alias, int myPort) {
        this.alias = alias;
        this.myPort = myPort;
        this.dataSemaphore = new Semaphore(1);

        this.ipSuccessor = "localhost";
        this.portSuccessor = myPort;
        this.ipPredecessor = "localhost";
        this.portPredecessor = myPort;
    }

    /**
    * Starts the program
    * @param args inputs to the chat program. 1st arg should be a name and 2nd arg should be a port above 4000.
    */
    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Parameter: <alias> <myPort>");
        }
        Chat chat = new Chat(args[0], Integer.parseInt(args[1]));

        // Initialization of the peer
        Thread server = new Thread(new Server(chat));
        Thread client = new Thread(new Client(chat));

        server.start();
        client.start();

        try {
            client.join();
            if(!client.isAlive()) System.exit(0);
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}