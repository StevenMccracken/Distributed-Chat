import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * This class represents the server who receives messages from the client via sockets.
 * It uses semaphores when accessing data shared by the client running in this process.
 */
public class Server implements Runnable {
    Chat chat;

    /**
     * Constructor for the Server class
     * @param chat the Chat object
     */
    public Server(Chat chat) {
        this.chat = chat;
    }

    /**
     * Performs the necessary actions for a server-side JOIN message in the Flooding protocol.
     * @param joinJson the message to send over the socket
     */
    public void join(JsonObject joinJson) {
        // Get necessary info from json message
        String clientAlias = joinJson.getJsonObject("parameters").getString("myAlias");
        Integer clientPort = joinJson.getJsonObject("parameters").getInt("myPort");

        System.out.printf("%s wants to join! Let's add them to our chat!%n", clientAlias);

        // Send accept message to client who tried to join
        JsonObject acceptJson = this.chat.createMessage_ACCEPT(this.chat.getIpPredecessor(), this.chat.getPortPredecessor());
        this.chat.sendJson(acceptJson, "localhost", clientPort);

        // Send new successor message to previous predecessor
        JsonObject successorJson = this.chat.createMessage_NEWSUCCESSOR("localhost", clientPort);
        this.chat.sendJson(successorJson, this.chat.getIpPredecessor(), this.chat.getPortPredecessor());

        // Update member variables for local client
        this.chat.updatePortPredecessor(clientPort);
    }

    /**
     * Performs the necessary actions for a server-side ACCEPT message in the Flooding protocol.
     * @param acceptJson the message to send over the socket
     */
    public void accept(JsonObject acceptJson) {
        System.out.printf("You've been accepted to the this.chat %s!%n", this.chat.getAlias());

        String ip = acceptJson.getJsonObject("parameters").getString("ipPred");
        Integer port = acceptJson.getJsonObject("parameters").getInt("portPred");

        this.chat.updateIpPredecessor(ip);
        this.chat.updatePortPredecessor(port);
    }

    /**
     * Performs the necessary actions for a server-side NEWSUCCESSOR message in the Flooding protocol.
     * @param successorJson the message to send over the socket
     */
    public void newSuccessor(JsonObject successorJson) {
        System.out.printf("Your new successor is at port %d%n", this.chat.getPortSuccessor());

        String ip = successorJson.getJsonObject("parameters").getString("ipSuccessor");
        Integer port = successorJson.getJsonObject("parameters").getInt("portSuccessor");

        this.chat.updateIpSuccessor(ip);
        this.chat.updatePortSuccessor(port);
    }

    /**
     * Performs the necessary actions for a server-side PUT message in the Flooding protocol.
     * @param putJson the message to send over the socket
     */
    public void put(JsonObject putJson) {
        String aliasSender = putJson.getJsonObject("parameters").getString("aliasSender");
        String aliasReceiver = putJson.getJsonObject("parameters").getString("aliasReceiver");

        // Message arrived back at sender which means receiver is not available
        if (aliasSender.equals(this.chat.getAlias())) {
            System.out.printf("%s is not available in this chat room%n", aliasReceiver);
        } else if (aliasReceiver.equals(this.chat.getAlias())) {
            // Message has arrived at correct place
            String message = putJson.getJsonObject("parameters").getString("message");
            System.out.printf("Message Alert! %s said '%s'%n", aliasSender, message);
        } else {
            // Pass message along circle
            System.out.println("I received a message but it's not for me!");
            this.chat.sendJson(putJson, this.chat.getIpSuccessor(), this.chat.getPortSuccessor());
        }
    }

    /**
     * Performs the necessary actions for a server-side LEAVE message in the Flooding protocol.
     * @param leaveJson the message to send over the socket
     */
    public void leave(JsonObject leaveJson) {
        System.out.println("Server received leave message");

        String ip = leaveJson.getJsonObject("parameters").getString("ipPred");
        Integer port = leaveJson.getJsonObject("parameters").getInt("portPred");

        this.chat.updateIpPredecessor(ip);
        this.chat.updatePortPredecessor(port);
    }

    /**
     * Main method of execution for the thread
     */
    public void run() {
        try {
            ServerSocket serverSocket = new ServerSocket(this.chat.getPort());
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Block until a client connects
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream()); // Get data from client socket

                // Read message from client
                JsonReader jsonreader = Json.createReader(ois);
                JsonObject jsonMessage = jsonreader.readObject();
                String responseType = jsonMessage.getString("type");

                switch(responseType) {
                    case "JOIN":
                        join(jsonMessage);
                        break;
                    case "ACCEPT":
                        accept(jsonMessage);
                        break;
                    case "NEWSUCCESSOR":
                        newSuccessor(jsonMessage);
                        break;
                    case "PUT":
                        put(jsonMessage);
                        break;
                    case "LEAVE":
                        leave(jsonMessage);
                        break;
                    default: System.out.printf("I received a JSON with an unknown type (%s)%n", responseType);
                }

                clientSocket.close(); // Close the current client connection to receive other client requests
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}