import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ChatServer {
    /// port number
    private static final int port = 5050;

    static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    static{
        try {
            FileHandler fileHandler =  new FileHandler("chat-server.log",true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);///INFO or FINE for more details
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


    private static ConcurrentHashMap<String, SocketChannel> clients = new ConcurrentHashMap<>();
    private static AtomicInteger clientIdCounter = new AtomicInteger(1);

    public static void main(String[] args) throws IOException {

        System.out.println("Chat server started on port " + port);
        ServerSocketChannel serverChannel = ServerSocketChannel.open(); /// creating server socket channel
        serverChannel.bind(new InetSocketAddress(port)); /// binding to port
        serverChannel.configureBlocking(false);/// non blocking mode

        Selector selector = Selector.open(); /// creating selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT); /// registering server for accepting

        /*
         * SELECTOR EXPLANATION:
         *
         * What is a Selector?
         * A Selector is a key component of Java NIO (Non-blocking I/O) that allows a single thread to monitor multiple
         * channels for I/O events. It acts as a multiplexer of SelectableChannel objects.
         *
         * How Selector works:
         * 1. Channels register with a Selector for specific events they're interested in (like accept, read, write)
         * 2. The Selector monitors all registered channels and identifies which ones are ready for operations
         * 3. This enables efficient handling of multiple connections without dedicating a thread to each connection
         *
         * In this code:
         * - We create a Selector with Selector.open()
         * - We register our ServerSocketChannel with this Selector for OP_ACCEPT events
         * - OP_ACCEPT means we're interested in accepting new client connections
         * - When selector.select() is called, it blocks until at least one channel is ready for its registered operation
         * - The Selector then provides a Set of SelectionKey objects representing the ready channels
         *
         * Benefits of using a Selector:
         * - Scalability: Can handle thousands of connections with a single thread
         * - Efficiency: Avoids thread creation/context switching overhead of the traditional thread-per-client model
         * - Resource management: Uses fewer system resources compared to blocking I/O with multiple threads
         *
         * The event loop (while(true) loop below) processes these ready channels by:
         * 1. Accepting new connections when a channel is acceptable (key.isAcceptable())
         * 2. Reading data when a channel has data available (key.isReadable())
         *
         * When we accept a new client, we register that client's channel with the same Selector but for OP_READ events,
         * so we'll be notified when that client sends data.
         */

        while (true) {
//           /*
//           ✅ Summary:
            //This block:
//	•	Waits for new connections or messages
//	•	Accepts new client connections
//	•	Registers each client for reading
//	•	Assigns them a username
//	•	Announces their arrival to the chat
//

//           */
            if (selector.select() == 0) continue; /// block until at least one channel is ready
            Set<SelectionKey> selectedKeys = selector.selectedKeys(); /// getting selected keys from selector>
            var iterator = selectedKeys.iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove(); ///Remove key to avoid processing the same key twice
                if (key.isAcceptable()) {
                    /// Accept new client
                    SocketChannel clientChannel = serverChannel.accept();
                    clientChannel.configureBlocking(false); //Non blocking
                    String clientId = "User" + clientIdCounter.getAndIncrement();///increment client id
                    clients.put(clientId, clientChannel);
                    clientChannel.register(selector, SelectionKey.OP_READ, clientId);

                    System.out.println();
                    logger.info(clientId + " has joined the chat from "+ clientChannel.getRemoteAddress());
                    broadcast(clientId, clientId +Colors.YELLOW.getCode()+  " has joined the chat");


                } else if (key.isReadable()) {
                    /*
                    * Code Part
Purpose
key.isReadable()
Check if client sent something
clientChannel.read(buffer)
Read bytes from client into buffer
bytesRead == -1
Detect graceful disconnection
buffer.flip() + decode()
Read and decode the message from buffer
handleMessage(...)
Process chat command or broadcast message

                    *
                    * */
                    SocketChannel clientChannel = (SocketChannel) key.channel();
                    String clientId = (String) key.attachment(); /// getting client id we attached earlier
                    ByteBuffer buffer = ByteBuffer.allocate(1024); /// creating buffer to store the income data

                    int bytesRead = -1; /// reading from client
                    try {

                        bytesRead = clientChannel.read(buffer); /// reading from client
                    } catch (IOException e) {
                        disconnect(clientChannel, key);
                        continue;
                    }

                    if (bytesRead == -1) {
                        disconnect(clientChannel, key);///want to disconnect that channel
                    } else {
                        buffer.flip();/// flip the buffer from writing to reading
                        String message = StandardCharsets.UTF_8.decode(buffer).toString().trim();
                        handleMessage(clientChannel, clientId, message, key);
                    }
                }
            }
        }

    }

    /*
Role
Sends to
broadcast()
Sends message to all except sender
All clients except one
send()
Sends message to one user only
One client’s channel
    Method

     */
    private static void broadcast(String fromUser, String message) {
        for (var client : clients.entrySet()) {
            if (!client.getKey().equals(fromUser)) {

                send(client.getValue(), fromUser + ": " + message);
            }
        }
    }

    private static void send(SocketChannel channel, String s) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap((s + "\n").getBytes(StandardCharsets.UTF_8));
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void handleMessage(SocketChannel senderChannel, String senderUsername, String message, SelectionKey key) {
        logger.info(senderUsername + " says: " + message);
        if (message.startsWith("/msg ")) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {

                send(senderChannel, Colors.RED.getCode() + "\n❌ Usage: /msg <recipient> <message>");
                return;
            }

            String recipient = parts[1];
            String privateMessage = parts[2];

            if (recipient.equals(senderUsername)) {
                send(senderChannel, Colors.RED.getCode() + "\n❌ You cannot message yourself.");
                return;
            }

            SocketChannel recipientChannel = clients.get(recipient);
            if (recipientChannel != null) {
                logger.info("Private message from " + senderUsername + " to " + recipient + ": " + privateMessage);
                send(recipientChannel, Colors.GREEN.getCode() + "🔒 Private from " + senderUsername + ": " + privateMessage);
                System.out.println();
                send(senderChannel, Colors.ORANGE.getCode()+"🔒 Private to " + recipient + ": " + privateMessage);

            } else {
                System.out.println();
                send(senderChannel,  Colors.RED.getCode()+"❌ User '" + recipient + "' not found.");
            }
            return;///
        }

        if (message.equals("/who")) {
            String users = String.join(", ", clients.keySet());
            System.out.println();
            send(senderChannel, Colors.BLUE.getCode()+"👥 Users online: " + users);
        }

        /// allow user to change their username when they join a chat
        if(message.startsWith("/nick")){
            String[]  parts = message.split(" ", 2);

            if(parts.length < 2 || parts[1].isEmpty()){
                send(senderChannel, Colors.RED.getCode() + "❌ Usage: /nick <newUsername>" + Colors.RESET.getCode());
            }
            String newUsername = parts[1];
            ///checking if the new username already exists
            if(clients.containsKey(newUsername)){
                send(senderChannel,  Colors.RED.getCode() + "❌ Username '" + newUsername + "' is already taken." + Colors.RESET.getCode());
                return;
            }

            /// Update clients map
        clients.remove(senderUsername); ///Remove the old name
        clients.put(newUsername, senderChannel);

        /// Notify other and the sender
        broadcast(senderUsername, Colors.YELLOW.getCode() + senderUsername + " is now known as " + newUsername + Colors.RESET.getCode());
        send(senderChannel, Colors.PURPLE.getCode() + "👤Your username has been changed to " + newUsername + Colors.RESET.getCode());

        /// Log the change
        logger.info(senderUsername + " changed their username to " + newUsername);
        }


        if (message.equals("/quit")) {
            System.out.println();
            send(senderChannel, Colors.RESET.getCode()+"👋 Goodbye!");
            System.out.println();
            disconnect(senderChannel, key); ///Cleanly closes the connection and removes user from maps
            key.cancel(); ///Cancels the selection key to stop tracking this channel
        }

        if (message.equals("/help")) {
            String help = """
                    🆘 Commands:
                    /msg <user> <message> - Send private message
                    /who - List online users
                    /quit - Leave chat
                    /help - Show this help
                    """ + Colors.CYAN.getCode();
            send(senderChannel, help);
        } else {
            broadcast(senderUsername, message);
        }
    }

    private static void disconnect(SocketChannel senderChannel, SelectionKey key) {
        try {
            String usernameToRemove = null;
            for (var client : clients.entrySet()) {
                if (client.getValue().equals(senderChannel)) {
                    usernameToRemove = client.getKey();
                    break;
                }
            }
            if (usernameToRemove != null) {
                logger.info(usernameToRemove + " has disconnected.");
                clients.remove(usernameToRemove);
                System.out.println();
                broadcast(usernameToRemove, " has left the chat 👋" + Colors.CYAN.getCode());
            }

            key.cancel();
            senderChannel.close();
        } catch (Exception e) {
            {
                logger.severe("Error reading from client: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}
