package chat;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * API do Chat UDP.
 * Gerencia o envio e recebimento de mensagens via protocolo UDP.
 * Esta classe não deve ser alterada (conforme especificação da atividade).
 */
public class Chat {

    private static final int BUFFER_SIZE = 4096;

    private final DatagramSocket socket;
    private final MessageContainer container;
    private boolean running;

    /**
     * Cria e inicia o Chat UDP na porta local especificada.
     *
     * @param localPort porta local para receber mensagens
     * @param container objeto que receberá as mensagens (view)
     * @throws IOException se não for possível criar o socket na porta informada
     */
    public Chat(int localPort, MessageContainer container) throws IOException {
        this.socket = new DatagramSocket(localPort);
        this.container = container;
        this.running = true;
        startReceiver();
    }

    /**
     * Envia uma mensagem para o endereço IP e porta remotos especificados.
     *
     * @param message    mensagem a ser enviada
     * @param remoteIp   endereço IP remoto
     * @param remotePort porta remota
     * @throws IOException se ocorrer erro ao enviar a mensagem
     */
    public void sendMessage(String message, String remoteIp, int remotePort) throws IOException {
        byte[] buffer = message.getBytes("UTF-8");
        InetAddress address = InetAddress.getByName(remoteIp);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, remotePort);
        socket.send(packet);
    }

    /**
     * Encerra o Chat, fechando o socket UDP.
     */
    public void close() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    /**
     * Inicia a thread receptora de mensagens.
     */
    private void startReceiver() {
        Thread receiverThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    String message = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
                    String senderIp = packet.getAddress().getHostAddress();
                    int senderPort = packet.getPort();
                    String fullMessage = "[" + senderIp + ":" + senderPort + "] " + message;
                    container.receiveMessage(fullMessage);
                } catch (IOException e) {
                    if (running) {
                        container.receiveMessage("[ERRO] Falha ao receber mensagem: " + e.getMessage());
                    }
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.setName("UDP-Receiver");
        receiverThread.start();
    }
}
