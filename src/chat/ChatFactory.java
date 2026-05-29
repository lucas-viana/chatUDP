package chat;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ChatFactory {
    private static int DEFAULT_RECEIVER_BUFFER_SIZE = 1000;
    private static String serverName;
    private static int serverPort;
    private static int localPort;
    private static MessageContainer container;
    private static Receiver receiver; // Keep track of the receiver to allow closing

    public static Sender build(boolean isConnectionOriented, String serverName,
                               int serverPort, int localPort, MessageContainer container)
            throws ChatException {
        ChatFactory.serverName = serverName;
        ChatFactory.serverPort = serverPort;
        ChatFactory.localPort = localPort;
        ChatFactory.container = container;
        return build(isConnectionOriented);
    }

    private static Sender build(boolean isConnectionOriented) throws ChatException {
        try {
            // TCP
            if (isConnectionOriented) {
                receiver = new TCPReceiver(ChatFactory.localPort, ChatFactory.container);
                return new TCPSender(InetAddress.getByName(ChatFactory.serverName), ChatFactory.serverPort);
            // UDP
            } else {
                receiver = new UDPReceiver(ChatFactory.localPort, DEFAULT_RECEIVER_BUFFER_SIZE, ChatFactory.container);
                return new UDPSender(InetAddress.getByName(ChatFactory.serverName), ChatFactory.serverPort);
            }
        } catch (UnknownHostException unknownHostException) {
            throw new ChatException("The receiver is unknown", unknownHostException);
        }
    }
    
    // Método adicionado para permitir que o ChatView pare o receiver
    public static void stopReceiver() {
        if (receiver != null) {
            receiver.close();
            receiver = null;
        }
    }
}
