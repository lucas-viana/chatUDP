package chat;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * API do Chat TCP.
 * Gerencia o envio e recebimento de mensagens via protocolo TCP (orientado a conexões).
 * Espelha a interface da classe Chat (UDP) para facilitar a troca de protocolo.
 */
public class ChatTCP {

    private static final int BUFFER_SIZE = 4096;

    private final ServerSocket serverSocket;
    private final MessageContainer container;
    private boolean running;

    /**
     * Cria e inicia o Chat TCP na porta local especificada.
     *
     * @param localPort porta local para receber conexões
     * @param container objeto que receberá as mensagens (view)
     * @throws IOException se não for possível criar o ServerSocket na porta informada
     */
    public ChatTCP(int localPort, MessageContainer container) throws IOException {
        this.serverSocket = new ServerSocket(localPort);
        this.container = container;
        this.running = true;
        startReceiver();
    }

    /**
     * Envia uma mensagem para o endereço IP e porta remotos especificados.
     * Cada envio abre uma conexão TCP, transmite a mensagem e fecha a conexão.
     *
     * @param message    mensagem a ser enviada
     * @param remoteIp   endereço IP remoto
     * @param remotePort porta remota
     * @throws IOException se ocorrer erro ao enviar a mensagem
     */
    public void sendMessage(String message, String remoteIp, int remotePort) throws IOException {
        try (Socket socket = new Socket(remoteIp, remotePort)) {
            OutputStream os = socket.getOutputStream();
            byte[] buffer = message.getBytes("UTF-8");
            os.write(buffer);
            os.flush();
            socket.shutdownOutput();
        }
    }

    /**
     * Encerra o Chat TCP, fechando o ServerSocket.
     */
    public void close() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException ignored) {}
    }

    /**
     * Inicia a thread receptora de conexões TCP.
     */
    private void startReceiver() {
        Thread receiverThread = new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Cada conexão é tratada em thread separada
                    Thread handler = new Thread(() -> handleClient(clientSocket));
                    handler.setDaemon(true);
                    handler.setName("TCP-Handler-" + clientSocket.getPort());
                    handler.start();
                } catch (IOException e) {
                    if (running) {
                        container.receiveMessage("[ERRO] Falha ao aceitar conexão TCP: " + e.getMessage());
                    }
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.setName("TCP-Receiver");
        receiverThread.start();
    }

    /**
     * Trata uma conexão de cliente: lê a mensagem completa e encaminha ao container.
     *
     * @param clientSocket socket do cliente conectado
     */
    private void handleClient(Socket clientSocket) {
        try {
            String senderIp = clientSocket.getInetAddress().getHostAddress();
            int senderPort = clientSocket.getPort();

            InputStream is = clientSocket.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            if (baos.size() > 0) {
                String message = baos.toString("UTF-8");
                String fullMessage = "[" + senderIp + ":" + senderPort + "] " + message;
                container.receiveMessage(fullMessage);
            }
        } catch (IOException e) {
            if (running) {
                container.receiveMessage("[ERRO] Falha ao ler mensagem TCP: " + e.getMessage());
            }
        } finally {
            try {
                clientSocket.close();
            } catch (IOException ignored) {}
        }
    }
}
