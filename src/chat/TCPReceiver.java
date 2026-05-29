package chat;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TCPReceiver implements Receiver {
    private int portNumber;
    private MessageContainer container;
    private ServerSocket receiverSocket;
    private boolean isRunning = false;

    public TCPReceiver(int portNumber, MessageContainer container) throws ChatException {
        this.portNumber = portNumber;
        this.container = container;
        try {
            prepare();
        } catch (IOException ioException) {
            throw new ChatException("There was some errors starting yor receiver.", ioException);
        }
        new Thread(this).start();
    }

    private void prepare() throws IOException {
        this.receiverSocket = new ServerSocket(this.portNumber);
    }

    public void run() {
        isRunning = true;
        try {
            // Adaptado para suportar múltiplos clientes (essencial para chat em grupo)
            while (isRunning) {
                Socket socket = this.receiverSocket.accept();
                
                Thread clientHandler = new Thread(() -> {
                    try {
                        DataInputStream inputFlow = new DataInputStream(socket.getInputStream());
                        while (isRunning) {
                            String message = inputFlow.readUTF();
                            String senderIp = socket.getInetAddress().getHostAddress();
                            int senderPort = socket.getPort();
                            container.newMessage("[" + senderIp + ":" + senderPort + "] " + message);
                        }
                    } catch (IOException e) {
                        // Conexão do cliente fechada
                    } finally {
                        try { socket.close(); } catch (IOException ignored) {}
                    }
                });
                clientHandler.setDaemon(true);
                clientHandler.start();
            }
        } catch (IOException ioException) {
            if (isRunning) {
                container.newMessage("There were some errors when receiving messages.");
            }
        }
    }
    
    @Override
    public void close() {
        isRunning = false;
        try {
            if (receiverSocket != null && !receiverSocket.isClosed()) {
                receiverSocket.close();
            }
        } catch (IOException ignored) {}
    }
}
