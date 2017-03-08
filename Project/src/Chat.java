import java.io.*;
import java.net.*;
import javax.json.*;
import java.util.Scanner;
import java.util.concurrent.Semaphore;

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

    // Lock
    public Semaphore semaphore;

    private JsonObject createMessage_JOIN(String alias, int myPort) {
        JsonObject joinJson = Json.createObjectBuilder()
                .add("type", "JOIN")
                .add("parameters", Json.createObjectBuilder()
                        .add("myAlias", alias)
                        .add("myPort", myPort))
                .build();
        return joinJson;
    }

    private JsonObject createMessage_ACCEPT(String ip, int port) {
        JsonObject acceptJson = Json.createObjectBuilder()
                .add("type", "ACCEPT")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipPred", ip)
                        .add("portPred", port))
                .build();
        return acceptJson;
    }

    private JsonObject createMessage_NEWSUCCESSOR(String ip, int port) {
        JsonObject newSuccessorJson = Json.createObjectBuilder()
                .add("type", "NEWSUCCESSOR")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipSuccessor", ip)
                        .add("portSuccessor", port))
                .build();
        return newSuccessorJson;
    }

    private JsonObject createMessage_PUT(String aliasSender, String aliasReceiver, String message) {
        JsonObject putJson = Json.createObjectBuilder()
                .add("type", "PUT")
                .add("parameters", Json.createObjectBuilder()
                        .add("aliasSender", aliasSender)
                        .add("aliasReceiver", aliasReceiver)
                        .add("message", message))
                .build();
        return putJson;
    }

    private JsonObject createMessage_LEAVE(String ip, int port) {
        JsonObject leaveJson = Json.createObjectBuilder()
                .add("type", "LEAVE")
                .add("parameters", Json.createObjectBuilder()
                        .add("ipPred", ip)
                        .add("portPred", port))
                .build();
        return leaveJson;
    }

    private class Server implements Runnable {
        public Server() {
        }

        public void run() {
            try {
                ServerSocket servSock = new ServerSocket(myPort);
                while (true) {
                    Socket clntSock = servSock.accept(); // Get client connections
                    ObjectInputStream ois   = new ObjectInputStream(clntSock.getInputStream());
                    ObjectOutputStream oos  = new ObjectOutputStream(clntSock.getOutputStream());

                    JsonReader jsonreader = Json.createReader(ois);
                    JsonObject jsonResponse = jsonreader.readObject();

                    String responseType = jsonResponse.getString("type");
                    if (responseType.equals("JOIN")) {
                        System.out.println("Receiving JOIN");

                        // Get necessary info from json sent from the client
                        String alias = jsonResponse.getJsonObject("parameters").getString("myAlias");
                        Integer port = jsonResponse.getJsonObject("parameters").getInt("myPort");

                        // Create new json responses with the necessary info
                        JsonObject acceptJson = createMessage_ACCEPT(ipPredecessor, portPredecessor);
                        JsonObject newSuccessorJson = createMessage_NEWSUCCESSOR("localhost", port);

                        clntSock.close(); // Close the client socket connection

                        // Open a new socket to flood the messages to update the routing table
                        Socket socket = new Socket("localhost", port);
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(acceptJson);
                        jsonWriter.close();
                        socket.close();

                        socket = new Socket("localhost", portPredecessor);
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(newSuccessorJson);
                        jsonWriter.close();
                        socket.close();

                        try {
                            semaphore.acquire(1);
                            ipPredecessor = "localhost";
                            portPredecessor = port;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release(1);
                        }
                    }
                    if (responseType.equals("ACCEPT")) {
                        System.out.println("Receiving ACCEPT");

                        String ip     = jsonResponse.getJsonObject("parameters").getString("ipPred");
                        Integer port  = jsonResponse.getJsonObject("parameters").getInt("portPred");

                        clntSock.close();

                        try {
                            semaphore.acquire(1);
                            ipPredecessor = ip;
                            portPredecessor = port;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release(1);
                        }
                    }
                    if (responseType.equals("NEWSUCCESSOR")) {
                        System.out.println("Receiving NEW SUCCESSOR");

                        String ip     = jsonResponse.getJsonObject("parameters").getString("ipSuccessor");
                        Integer port  = jsonResponse.getJsonObject("parameters").getInt("portSuccessor");

                        clntSock.close();

                        try {
                            semaphore.acquire(1);
                            ipSuccessor = ip;
                            portSuccessor = port;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release(1);
                        }
                    }
                    if (responseType.equals("PUT")) {
                        String aliasSender = jsonResponse.getJsonObject("parameters").getString("aliasSender");
                        String aliasReceiver = jsonResponse.getJsonObject("parameters").getString("aliasReceiver");
                        String message = jsonResponse.getJsonObject("parameters").getString("message");

                        // Message arrived back at sender which means receiver is not available
                        if (aliasSender.equals(alias)) {
                            System.out.printf("%s is not available%n", aliasReceiver);
                        } else if (aliasReceiver.equals(alias)) {
                            // Message has arrived at correct place
                            System.out.printf("%s: %s%n", aliasSender, message);
                        } else {
                            // Send message along circle
                            Socket socket = new Socket(ipSuccessor, portSuccessor);
                            oos = new ObjectOutputStream(socket.getOutputStream());
                            JsonWriter jsonWriter = Json.createWriter(oos);
                            jsonWriter.write(jsonResponse);
                            jsonWriter.close();
                            socket.close();
                        }
                    }
                    if (responseType.equals("LEAVE")) {
                        System.out.println("Receiving LEAVE");

                        String ip     = jsonResponse.getJsonObject("parameters").getString("ipPred");
                        Integer port  = jsonResponse.getJsonObject("parameters").getInt("portPred");

                        clntSock.close();

                        try {
                            semaphore.acquire(1);
                            ipPredecessor = ip;
                            portPredecessor = port;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release(1);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Client implements Runnable {
        public Client() {
        }

        public void run() {
            while (true) {
                int port;
                int option = getMenuOption();

                // JOIN
                if (option == 1) {
                    Scanner s = new Scanner(System.in);
                    System.out.println("IP address you want to connect to?");
                    String ip = s.next();

                    System.out.println("Port you want to connect to?");
                    port = s.nextInt();

                    try {
                        Socket socket = new Socket(ip, port);
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonObject join = createMessage_JOIN(alias, myPort);
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(join);
                        jsonWriter.close();
                        socket.close();

                        try {
                            semaphore.acquire(1);
                            portSuccessor = port;
                            ipSuccessor = ip;
                        } catch(InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            semaphore.release(1);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // PRINT
                if (option == 2) {
                    System.out.println("Successor " + portSuccessor);
                    System.out.println("Predecessor " + portPredecessor);
                }

                // PUT
                if (option == 3) {
                    Scanner s = new Scanner(System.in);
                    System.out.println("Who do you want to message? ");
                    String to = s.nextLine();

                    System.out.printf("What do you want to say to %s?%n", to);
                    String message = s.nextLine();

                    try {
                        JsonObject putMessage = createMessage_PUT(alias, to, message);
                        Socket socket = new Socket(ipSuccessor, portSuccessor);
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(putMessage);
                        jsonWriter.close();
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if(option == 4) {
                    try {
                        JsonObject successorMessage = createMessage_NEWSUCCESSOR(ipSuccessor, portSuccessor);
                        Socket socket = new Socket(ipPredecessor, portPredecessor);
                        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                        JsonWriter jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(successorMessage);
                        jsonWriter.close();
                        socket.close();

                        JsonObject leaveMessage = createMessage_LEAVE(ipPredecessor, portPredecessor);
                        socket = new Socket(ipSuccessor, portSuccessor);
                        oos = new ObjectOutputStream(socket.getOutputStream());
                        jsonWriter = Json.createWriter(oos);
                        jsonWriter.write(leaveMessage);
                        jsonWriter.close();
                        socket.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public int getMenuOption() {
            String[] options = new String[4];
            options[0] = "Join";
            options[1] = "Print info";
            options[2] = "Send a message";
            options[3] = "Leave";

            System.out.printf("--------------------%nChoose an option!%n--------------------%n");
            for(int i = 0; i < options.length; i++)
                System.out.printf("%d) %s%n",i+1,options[i]);

            Scanner in = new Scanner(System.in);
            int choice = in.nextInt();
            while(choice < 1 || choice > options.length) {
                System.out.printf("%d is an invalid option. Please try again%n", choice);
                choice = in.nextInt();
            }

            return choice;
        }
    }

    public Chat(String alias, int myPort) {
        this.alias = alias;
        this.myPort = myPort;
        this.semaphore = new Semaphore(1);

        this.ipSuccessor = "localhost";
        this.portSuccessor = myPort;
        this.ipPredecessor = "localhost";
        this.portPredecessor = myPort;

        // Initialization of the peer
        Thread server = new Thread(new Server());
        Thread client = new Thread(new Client());
        server.start();
        client.start();
        try {
            client.join();
            server.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Parameter: <alias> <myPort>");
        }
        Chat chat = new Chat(args[0], Integer.parseInt(args[1]));
    }
}