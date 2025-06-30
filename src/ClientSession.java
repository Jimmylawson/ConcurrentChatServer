import java.io.ByteArrayOutputStream;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class ClientSession {

    private String username;
    private boolean isSendingFile = false;
    private boolean isReceivingFile = false;
    private String fileReceipient;
    private String fileName;
    private int fileSize;
    private ByteArrayOutputStream fileOutputStream;
    private boolean receivingFile;
    private int expectedFileSize;
    private ByteArrayOutputStream fileBuffer;
    private String incomingFileName;

   public ClientSession(String username){
       this.username = username;
   }

    public void startReceivingFile(String fileName, int fileSize) {
        this.receivingFile = true;
        this.expectedFileSize = fileSize;
        this.incomingFileName = fileName;
        this.fileBuffer = new ByteArrayOutputStream();
    }

    public int getExpectedFileSize() {
        return expectedFileSize;
    }

    public void setExpectedFileSize(int expectedFileSize) {
        this.expectedFileSize = expectedFileSize;
    }

    public ByteArrayOutputStream getFileBuffer() {
        return fileBuffer;
    }

    public void setFileBuffer(ByteArrayOutputStream fileBuffer) {
        this.fileBuffer = fileBuffer;
    }

    public String getIncomingFileName() {
        return incomingFileName;
    }

    public void setIncomingFileName(String incomingFileName) {
        this.incomingFileName = incomingFileName;
    }

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isSendingFile() {
        return isSendingFile;
    }

    public void setSendingFile(boolean sendingFile) {
        isSendingFile = sendingFile;
    }

    public boolean isReceivingFile() {
        return isReceivingFile;
    }

    public void setReceivingFile(boolean receivingFile) {
        isReceivingFile = receivingFile;
    }

    public String getFileReceipient() {
        return fileReceipient;
    }

    public void setFileReceipient(String fileReceipient) {
        this.fileReceipient = fileReceipient;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public ByteArrayOutputStream getFileOutputStream() {
        return fileOutputStream;
    }

    public void setFileOutputStream(ByteArrayOutputStream fileOutputStream) {
        this.fileOutputStream = fileOutputStream;
    }
}
