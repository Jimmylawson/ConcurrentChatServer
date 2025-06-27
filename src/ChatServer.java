import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    private static final int port = 5050; /// port number
    private static ConcurrentHashMap<String, PrintWriter> clients  = new ConcurrentHashMap<>();
    private static AtomicInteger clientIdCounter  = new AtomicInteger(1);
    public static void main(String[] args) throws IOException {

        System.out.println("Chat server started on port " + port);
        ServerSocket serverSocket = new ServerSocket(port);

        /// Creating threads for each client
        ExecutorService executorService = Executors.newCachedThreadPool();

        while(true){
            Socket clientSocket = serverSocket.accept();///accepting client connection
            String clientId = "UserId: " + clientIdCounter.getAndIncrement();///increment client id
            executorService.execute(new ClientHandler(clients,clientSocket,clientId));
        }




    }
}
