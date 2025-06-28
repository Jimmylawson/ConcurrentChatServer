//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.

//
//import java.io.*;
//import java.net.Socket;
//import java.util.concurrent.ConcurrentHashMap;
//
//public class ClientHandler implements Runnable {
//   private ConcurrentHashMap<String, PrintWriter> clients;
//   private Socket socket;
//   private String clientId;
//   private PrintWriter out; ///for writing
//   private BufferedReader in; ///for reading
//
//    public ClientHandler(ConcurrentHashMap<String, PrintWriter> clients, Socket socket, String clientId) {
//        this.clients = clients;
//        this.socket = socket;
//        this.clientId = clientId;
//    }
//
//    /**
//     * When an object implementing interface {@code Runnable} is used
//     * to create a thread, starting the thread causes the object's
//     * {@code run} method to be called in that separately executing
//     * thread.
//     * <p>
//     * The general contract of the method {@code run} is that it may
//     * take any action whatsoever.
//     *
//     * @see Thread#run()
//     */
//    @Override
//    public void run() {
//        try{
//            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // reading message from the client
//            out = new PrintWriter(socket.getOutputStream(),true);/// reading  message to the client
//            clients.put(clientId, out);
//
//            broadcast(clientId + " has joined the chat");
//            String message = "";
//
//
//            while((message = in.readLine()) != null){
//                /// /quit will ->gracefully leave the chat
//                if(message.equalsIgnoreCase("/quit")){
//                    out.println(clientId + " has left the chat👋");
//                    break;
//                }
//                /// help will return the list of commands
//            if(message.equalsIgnoreCase("/help")){
//                out.println("\n💬Available commands:");
//                out.println("/help - Show this help message");
//                out.println("/who - List all users online");
//                out.println("/msg <recipient> <message> - Send a private message to a user");
//                out.println("/quit - Leave the chat");
//            }
//                /// /who will return the list of users
//                if(message.equalsIgnoreCase("/who")){
//                    out.println("👥 Users online : " + String.join(", ",clients.keySet()));
//                }
//                /// Check if its a private message
//                if(message.startsWith("/msg")){
//                    String[] parts  = message.split(" ", 3);
//                    if(parts.length < 3){
//                        out.println("❌ Usage: /msg <recipient> <message>");
//                        continue;
//                    }
//                    String recipient = parts[1];
//                    String privateMessage = parts[2];
//
//                    PrintWriter recipientOut = clients.get(recipient);///get the writer of the recipient
//
//                if(recipient.equals(clientId)){
//                    out.println("❌ You cannot send a private message to yourself.");
//                    continue;
//                }
//
//                    if(recipientOut != null){
//                        /// Send to recipient
//                        recipientOut.println("🔒 Private from " + clientId + ": " + privateMessage);
//
//                        // Echo back to sender
//                        out.println("🔒 Private to " + recipient + ": " + privateMessage);
//                    }else{
//                        out.println("❌ User '" + recipient + "' not found.");
//                    }
//                }else broadcast(clientId + ": " + message);
//
//            }
//
//        }catch(IOException e){
//            System.out.println(clientId + " disconnected.");
//        }finally{
//            try{ socket.close();}catch(Exception e){ e.printStackTrace();}
//            clients.remove(clientId);
//            broadcast(clientId + " has left the chat");
//        }
//    }
//
//
//    public void broadcast(String message){
//        for(PrintWriter writer : clients.values()){
//            writer.println(message);
//        }
//    }
//
//
//}