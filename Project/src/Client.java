import java.util.Scanner;
import javax.json.JsonObject;

/**
 * This class represents the client who sends messages to the server via sockets.
 */
public class Client implements Runnable {
    Chat chat;

    /**
     * Constructor for the Client class
     * @param chat the Chat object
     */
    public Client(Chat chat) {
        this.chat = chat;
    }

    /**
     * Performs the necessary actions for a client-side JOIN message in the Flooding protocol.
     */
    public void join() {
        int port = getIntegerInput("What port do you want to connect to " + this.chat.getAlias() + "?\n");

        JsonObject joinJson = this.chat.createMessage_JOIN(this.chat.getAlias(), this.chat.getPort());
        this.chat.sendJson(joinJson, "localhost", port);

        // Make successor of this local user equal to the port the user wished to connect to
        this.chat.updatePortSuccessor(port);
    }

    /**
     * Performs the necessary actions for a client-side PUT message in the Flooding protocol.
     */
    public void put() {
        String recipient = getTextInput("Who do you want to message? ");
        if(recipient.equals(this.chat.getAlias())) {
            System.out.printf("You can't message yourself!%n");
            return;
        }
        String message = getTextInput("What do you want to say to " + recipient + "? ");

        JsonObject putJson = this.chat.createMessage_PUT(this.chat.getAlias(), recipient, message);
        this.chat.sendJson(putJson, this.chat.getIpSuccessor(), this.chat.getPortSuccessor());
    }

    /**
     * Performs the necessary actions for a client-side LEAVE message in the Flooding protocol.
     */
    public void leave() {
        String ipSuccessor = this.chat.getIpSuccessor(), ipPredecessor = this.chat.getIpPredecessor();
        int portSuccessor = this.chat.getPortSuccessor(), portPredecessor = this.chat.getPortPredecessor();

        // Let this local user's predecessor know that the new successor is this local user's current successor
        JsonObject successorJson = this.chat.createMessage_NEWSUCCESSOR(ipSuccessor, portSuccessor);
        this.chat.sendJson(successorJson, ipPredecessor, portPredecessor);

        // Let this local user's successor know that the new predecessor is this local user's current predecessor
        JsonObject leaveJson = this.chat.createMessage_LEAVE(ipPredecessor, portPredecessor);
        this.chat.sendJson(leaveJson, ipSuccessor, portSuccessor);
    }

    /**
     * Main method of execution for the thread
     */
    public void run() {
        boolean alive = true;
        while (alive) {
            int option = getMenuOption();
            switch(option) {
                case 1:
                    join();
                    break;
                case 2:
                    put();
                    break;
                case 3:
                    System.out.printf("Successor: %s%nPredecessor: %s%n", this.chat.getPortSuccessor(), this.chat.getPortPredecessor());
                    break;
                case 4:
                    leave();
                    alive = false;
                    break;
                default:
            }
        }
        System.out.printf("Goodbye %s!%n", this.chat.getAlias());
    }

    /**
     * This method displays a list of menu options for the user to input.
     * It repeatedly asks for user input until it is valid.
     * @return the valid menu choice that the user entered
     */
    public int getMenuOption() {
        // Create the options
        String[] options = new String[4];
        options[0] = "Join";
        options[1] = "Send a message";
        options[2] = "Print info";
        options[3] = "Leave";

        // Print the options
        System.out.printf("--------------------%nChoose an option!%n--------------------%n");
        for(int i = 0; i < options.length; i++)
            System.out.printf("%d) %s%n",i+1,options[i]);

        // Get the user's option choice
        int choice = getIntegerInput();
        while(choice < 1 || choice > options.length) {
            choice = getIntegerInput(choice + " is an invalid option. Please try again\n");
        }

        return choice;
    }

    /**
     * Gets user string input
     * @return the string the user enters
     */
    public String getTextInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    /**
     * Gets user string input
     * @param message the prompt to display to the user
     * @return the string the user enters
     */
    public String getTextInput(String message) {
        System.out.println(message);
        return getTextInput();
    }

    /**
     * Gets user integer input
     * @return the integer the user enters
     */
    public int getIntegerInput() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextInt();
    }

    /**
     * Gets user integer input
     * @param message the prompt to display to the user
     * @return the integer the user enters
     */
    public int getIntegerInput(String message) {
        System.out.print(message);
        return getIntegerInput();
    }
}