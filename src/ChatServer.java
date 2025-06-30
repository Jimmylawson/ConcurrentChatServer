import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
    private static final Map<SocketChannel,ClientSession> clientSessions = new ConcurrentHashMap<>();///for file transfer

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

// Main server loop that continuously processes client connections and messages
        while (true) {
            // Wait until there's at least one channel ready for I/O operations
            // Returns 0 if no channels are ready, in which case we continue waiting
            if (selector.select() == 0) continue;

            // Get the set of keys that represent channels ready for operations
            // These could be new connections or data ready to be read
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            var iterator = selectedKeys.iterator();

            // Process each ready channel one by one
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                // Remove the current key from the set to prevent processing it again
                // This is important because Selector doesn't remove keys automatically
                iterator.remove();

                // Check if this key represents a new connection request
                if (key.isAcceptable()) {
                    // Accept the new client connection and get their channel
                    SocketChannel clientChannel = serverChannel.accept();
                    // Configure the client channel to be non-blocking
                    // This allows us to handle multiple clients without dedicated threads
                    clientChannel.configureBlocking(false);

                    // Generate a unique temporary ID for the new client
                    // Format: "User1", "User2", etc.
                    String clientId = "User" + clientIdCounter.getAndIncrement();

                    // Store the client's channel in our clients map
                    clients.put(clientId, clientChannel);

                    // Register this client channel with the selector for READ operations
                    // This means we'll be notified when this client sends messages
                    // We also attach the clientId to the key for later reference
                    clientChannel.register(selector, SelectionKey.OP_READ, clientId);

                    // Add blank line for better console readability
                    System.out.println();

                    // Log the new connection with client's address information
                    logger.info(clientId + " has joined the chat from " + clientChannel.getRemoteAddress());

                    // Announce to all other clients that someone new has joined
                    // The message is colored yellow for visibility
                    broadcast(clientId, clientId + Colors.YELLOW.getCode() + " has joined the chat");
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

                        bytesRead = clientChannel.read(buffer); /// reading from client into buffer
                    ClientSession clientSession = clientSessions.get(clientChannel); ///Get session associated with this client
                    if(clientSession != null && clientSession.isSendingFile()){
                        /// If this client is currently sending a file ..
                        ByteArrayOutputStream output = clientSession.getFileOutputStream();
                        output.write(buffer.array(), 0, bytesRead);



                        // Check if file is fully received
                        if (output.size() >= clientSession.getFileSize()) {
                            clientSession.setSendingFile(false);

                            /// Save file to disk

                            try (FileOutputStream fos = new FileOutputStream("received_" + clientSession.getFileName())) {
                                output.writeTo(fos);
                                logger.info("📁 File saved to disk as: received_" + clientSession.getFileName());
                            } catch (IOException e) {
                                logger.severe("❌ Failed to save file: " + e.getMessage());
                            }

                            // Forward file to recipient
                            SocketChannel recipientChannel = clients.get(clientSession.getFileReceipient());
                            if (recipientChannel != null) {
                                send(recipientChannel, Colors.ORANGE.getCode() + "📥 You received file '" + clientSession.getFileName() + "' from " + clientSession.getUsername());
                                recipientChannel.write(ByteBuffer.wrap(output.toByteArray()));
                            }

                            send(clientChannel, Colors.GREEN.getCode() + "✅ File '" + clientSession.getFileName() + "' sent successfully");
                        }

                        return; // Skip normal message handling
                    }
                    } catch (IOException e) {
                        disconnect(clientChannel, key);
                        continue;
                    }

                    if (bytesRead == -1) {
                        disconnect(clientChannel, key);///want to disconnect that channel
                    } else {
                        buffer.flip();/// flip the buffer from writing to reading
                        String message = StandardCharsets.UTF_8.decode(buffer).toString().trim();


                        /// Check if this is a new client
                    if(!clientSessions.containsKey(clientChannel)){
                        /// Assigning the maybe the new message
                        String username = message;
                        clients.put(username, clientChannel);
                        var session = new ClientSession(username);
                        clientSessions.put(clientChannel, session);
                        logger.info(username + " has joined from " + clientChannel.getRemoteAddress());
                        broadcast(username, Colors.GREEN.getCode() + "🎉 " + username + " has joined the chat!" + Colors.RESET.getCode());
                        return; // don't handle this as a regular chat message
                    }
                        handleMessage(clientChannel, clientId, message, key);
                    }
                }
            }
        }

    }

    // Method to send a message to all clients except the sender
    private static void broadcast(String fromUser, String message) {
        // Iterate through all clients in the clients map
        // clients.entrySet() contains username-channel pairs
        for (var client : clients.entrySet()) {
            // Skip sending the message back to the original sender
            // by comparing usernames (stored as the key in the map)
            if (!client.getKey().equals(fromUser)) {
                // Send the message to each other client
                // client.getValue() gets the SocketChannel for this client
                // Format the message as "fromUser: message"
                send(client.getValue(), fromUser + ": " + message);
            }
        }
    }

    // Method to send a message to a specific client through their socket channel
    private static void send(SocketChannel channel, String s) {
        try {
            // Create a ByteBuffer containing the message:
            // 1. Add a newline character to the message for proper line breaks
            // 2. Convert the string to bytes using UTF-8 encoding
            // 3. Wrap these bytes in a ByteBuffer for NIO operations
            ByteBuffer buffer = ByteBuffer.wrap((s + "\n").getBytes(StandardCharsets.UTF_8));

            // Write the contents of the buffer to the client's channel
            // This sends the message over the network to the client
            channel.write(buffer);
        } catch (IOException e) {
            // If there's an error during sending (e.g., client disconnected),
            // print the stack trace for debugging purposes
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

        if(message.startsWith("/sendfile")){
            String[] sessionParts = message.split(" ",4);
            if(sessionParts.length < 4){
                send(senderChannel, Colors.RED.getCode() + "\n❌ Usage: /sendfile <user> <filename> <filesize>");
                return;

            }

            String receiver = sessionParts[1];
            String fileName = sessionParts[2];
            int fileSize = Integer.parseInt(sessionParts[3]);

            SocketChannel receiverChannel = clients.get(receiver);
            if(receiverChannel == null){
                send(senderChannel,Colors.RED.getCode() + "\n❌ User '" + receiver + "' not found.");
                return;
            }

            var senderSession  = clientSessions.get(senderChannel);
            senderSession.setSendingFile(true);
            senderSession.setFileName(fileName);
            senderSession.setFileReceipient(receiver);
            senderSession.setFileOutputStream(new ByteArrayOutputStream());
            senderSession.setFileSize(fileSize);

            /// Receiver session setup
        var receiverSession = clientSessions.get(receiverChannel);
            senderSession.startReceivingFile(fileName,fileSize);
            send(senderChannel, Colors.GREEN.getCode()+"📤 Ready to send file: " + fileName);
            send(receiverChannel, Colors.GREEN.getCode()+"📥 " + senderSession.getUsername() + " is sending you a file: " + fileName);

            return;
        }


        if (message.equals("/who")) {
            String users = String.join(", ", clients.keySet());
            System.out.println();
            send(senderChannel, Colors.BLUE.getCode()+"👥 Users online: " + users);

            return;
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
            return;
        }


        if (message.equals("/quit")) {
            System.out.println();
            send(senderChannel, Colors.RESET.getCode()+"👋 Goodbye!");
            System.out.println();
            disconnect(senderChannel, key); ///Cleanly closes the connection and removes user from maps
            key.cancel(); ///Cancels the selection key to stop tracking this channel

        return;
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
            return;
        }

            broadcast(senderUsername, message);


    }

    // Method to handle client disconnection and cleanup
    private static void disconnect(SocketChannel senderChannel, SelectionKey key) {
        try {
            // Initialize variable to store the username of disconnecting client
            String usernameToRemove = null;
            
            // Search through all clients to find the username associated with this channel
            // We need to search by channel (value) to find the username (key)
            for (var client : clients.entrySet()) {
                if (client.getValue().equals(senderChannel)) {
                    usernameToRemove = client.getKey();
                    break;  // Exit loop once we find the matching client
                }
            }

            // If we found the username of the disconnecting client
            if (usernameToRemove != null) {
                // Log the disconnection event to server logs
                logger.info(usernameToRemove + " has disconnected.");
                
                // Remove the client from our active clients map
                clients.remove(usernameToRemove);
                
                // Add blank line for console readability
                System.out.println();
                
                // Notify all other clients that this user has left
                // Message includes a wave emoji and is colored cyan
                broadcast(usernameToRemove, " has left the chat 👋" + Colors.CYAN.getCode());
            }

            // Cancel the selection key to stop monitoring this channel
            key.cancel();
            
            // Close the socket channel to free system resources
            senderChannel.close();
            
        } catch (Exception e) {
            // If any error occurs during disconnection process
            // Log it as a severe error with the error message
            logger.severe("Error reading from client: " + e.getMessage());
            // Print the full stack trace for debugging
            e.printStackTrace();
        }
    }


}