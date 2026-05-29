package chat;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

public class TCPSender implements Sender {
    private Socket socket = null;
    private DataOutputStream outputFlow;
    private InetAddress receiver;

    public TCPSender(InetAddress receiver, int receiverPort) throws ChatException {
        this.receiver = receiver;
        try {
            boolean isConnected = false;
            while (!isConnected) {
                try {
                    this.socket = new Socket(this.receiver, receiverPort);
                    isConnected = true;
                } catch (ConnectException connectException) {
                    // Evita consumir 100% da CPU em loop infinito falhando
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
            this.outputFlow = new DataOutputStream(this.socket.getOutputStream());
        } catch (IOException ioException) {
            throw new ChatException("There was some errors starting your sender.", ioException);
        }
    }

    public void send(String message) throws ChatException {
        try {
            this.outputFlow.writeUTF(message);
        } catch (IOException ioException) {
            throw new ChatException("There was some sending your message", ioException);
        }
    }
}
