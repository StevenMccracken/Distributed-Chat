import java.net.*;
import java.io.*;
import java.util.*;
import javax.json.*;
/*****************************//**
 * \brief It implements a distributed chat.
 * It creates a ring and delivers messages
 * using flooding
 **********************************/
public class Chat {

    // My info
    public String alias;
    public int myPort;
    // Successor
    public String ipSuccessor;
    public int portSuccessor;
    // Predecessor
    public String ipPredecessor;
    public int portPredecessor;


    private JsonObject createJOINmsg(String alias, int myPort) {
        JsonObject value = Json.createObjectBuilder()
                .add("type", "JOIN")
                .add("parameters", Json.createObjectBuilder()
                        .add("myAlias", alias)
                        .add("myPort", myPort))
                .build();
        return value;
    }

    private JsonObject createACCEPTmsg(String ip, int port) {
        JsonObject value = Json.createObjectBuilder()
                .add("type", "ACCEPT")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipPred", ip)
                        .add("portPred", port))
                .build();
        return value;
    }


    private JsonObject createNEWSUCmsg(String ip, int port) {
        JsonObject value = Json.createObjectBuilder()
                .add("type", "NEWSUCCESSOR")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipSuccessor", ip)
                        .add("portSuccessor", port))
                .build();
        return value;
    }

    private JsonObject createPUTmsg(String aliasSender, String aliasReceiver, String message) {
        JsonObject value = Json.createObjectBuilder()
                .add("type", "PUT")
                .add("parameters", Json.createObjectBuilder()
                        .add("aliasSender", aliasSender)
                        .add("aliasReceiver", aliasReceiver)
                        .add("message", message))
                .build();
        return value;
    }

    private JsonObject createLEAVEmsg(String ip, int port) {
        JsonObject value = Json.createObjectBuilder()
                .add("type", "LEAVE")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipPred", ip)
                        .add("portPred", port))
                .build();
        return value;
    }

    /*****************************//**
     * \class Server class "chat.java"
     * \brief It implements the server
     **********************************/
    private class Server implements Runnable {
        public Server() {}
        /*****************************//**
        * \brief It allows the system to interact with the participants.
        * **********************************/
        public void run() {
            try {
                ServerSocket servSock = new ServerSocket(myPort);
                while (true) {
                    Socket clntSock = servSock.accept(); // Get client connections

                    ObjectInputStream  ois = new ObjectInputStream(clntSock.getInputStream());
                    ObjectOutputStream oos = new ObjectOutputStream(clntSock.getOutputStream());
                    JsonReader jsonreader = Json.createReader(ois);

                    JsonObject value = jsonreader.readObject();

                    if (value.getString("type").equals("JOIN")) {
                        System.out.println("Receiving Joining ");

                        String alias = value.getJsonObject("parameters").getString("myAlias");
                        Integer port = value.getJsonObject("parameters").getInt("myPort");

                        JsonObject accept = createACCEPTmsg(ipPredecessor, portPredecessor);
                        JsonObject newSuccessor = createNEWSUCmsg("localhost", port);

                        clntSock.close();

                        Socket socket = new Socket("localhost", port);
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(accept);
                        jsonWriter.close();
                        socket.close();

                        socket = new Socket("localhost", portPredecessor);
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(newSuccessor);
                        jsonWriter.close();
                        socket.close();


                        ipPredecessor = "localhost";
                        portPredecessor = port;
                    }
                    if (value.getString("type").equals("ACCEPT")) {
                        System.out.println("Receiving Accept ");

                        String ip = value.getJsonObject("parameters").getString("ipPred");
                        Integer port = value.getJsonObject("parameters").getInt("portPred");

                        clntSock.close();

                        ipPredecessor = ip;
                        portPredecessor = port;
                    }
                    if (value.getString("type").equals("NEWSUCCESSOR")) {
                        System.out.println("Receiving new successor ");

                        String ip = value.getJsonObject("parameters").getString("ipSuccessor");
                        Integer port = value.getJsonObject("parameters").getInt("portSuccessor");

                        clntSock.close();

                        ipSuccessor = ip;
                        portSuccessor = port;
                    }
                }
            } catch (IOException e) {
                System.out.println(e.toString());
            }
        }
    }



    /*****************************//*
    * \brief It implements the client
    **********************************/
    private class Client implements Runnable {
        public Client() {}

        /*****************************//**
         * \brief It allows the user to interact with the system.
         **********************************/
        public void run() {
            while (true) {
                int port;
                System.out.println("Option");
                Scanner s = new Scanner(System.in);
                int option = s.nextInt();

                // JOIN
                if (option == 1) {
                    System.out.println("IP address? ");
                    String ip = s.next();

                    System.out.println("Port? ");
                    port = s.nextInt();

                    ipSuccessor = ip;
                    portSuccessor = port;

                    try {
                        Socket socket = new Socket(ipSuccessor, portSuccessor);
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonObject join = createJOINmsg("steve", myPort);
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(join);
                        jsonWriter.close();
                        socket.close();
                        portSuccessor = port;
                        ipSuccessor = "localhost";
                    } catch(IOException e) {
                        System.out.println(e.toString());
                    }
                }

                // PRINT
                if (option == 2) {
                    System.out.println("Successor "+portSuccessor);
                    System.out.println("Predecessor "+portPredecessor);
                }

              /*
              Create a simple user interface

              The first thing to do is to join ask the ip and port when joining and set ipSuccessor = ip, portSuccessor = port
              Socket socket = new Socket(ipSuccessor, portSuccessor);


              // Create the mssages m using JsonWriter and send it as stream

              ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
              ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
              oos.write(m);   this sends the message
                ois.read();    reads the response and parse it using JsonParser
              socket.close();

               Use mutex to handle race condition when reading and writing the global variable (ipSuccessor,
                    portSuccessor, ipPredecessor, portPredecessor)

               */
            }
        }
    }

    /*****************************//**
     * Starts the threads with the client and server:
     * \param Id unique identifier of the process
     * \param port where the server will listen
     **********************************/
    public Chat(String alias, int myPort) {
        this.alias = alias;
        this.myPort = myPort;

        ipSuccessor = "localhost";
        portSuccessor = myPort;
        ipPredecessor= "localhost";
        portPredecessor = myPort;

        // Initialization of the peer
        Thread server = new Thread(new Server());
        Thread client = new Thread(new Client());
        server.start();
        client.start();
        try {
            client.join();
            server.join();
        } catch (InterruptedException e) {
            System.out.println(e.toString());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2 ) {
            throw new IllegalArgumentException("Parameter: <alias> <myPort>");
        }
        Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
    }
}