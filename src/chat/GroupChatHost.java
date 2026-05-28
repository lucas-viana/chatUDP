package chat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Gerencia o chat em grupo no lado do host.
 * Atua como intermediário entre a API de chat (UDP/TCP) e a view.
 * Recebe mensagens, replica para todos os peers registrados e encaminha para a view.
 *
 * Protocolo de controle:
 * - ##REGISTER##<porta>   → registra o peer (IP extraído do cabeçalho da mensagem)
 * - ##UNREGISTER##<porta> → remove o peer do grupo
 */
public class GroupChatHost implements MessageContainer {

    private static final String REGISTER_PREFIX = "##REGISTER##";
    private static final String UNREGISTER_PREFIX = "##UNREGISTER##";

    private final MessageContainer viewContainer;
    private Chat chatUdp;
    private ChatTCP chatTcp;
    private boolean useTcp;
    private final List<Peer> peers = new CopyOnWriteArrayList<>();

    /**
     * Representa um peer (cliente) registrado no grupo.
     */
    private static class Peer {
        final String ip;
        final int port;

        Peer(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Peer)) return false;
            Peer peer = (Peer) o;
            return port == peer.port && ip.equals(peer.ip);
        }

        @Override
        public int hashCode() {
            return ip.hashCode() * 31 + port;
        }

        @Override
        public String toString() {
            return ip + ":" + port;
        }
    }

    /**
     * Cria o GroupChatHost com a view como destino final das mensagens.
     *
     * @param viewContainer a view que exibirá as mensagens
     */
    public GroupChatHost(MessageContainer viewContainer) {
        this.viewContainer = viewContainer;
    }

    /**
     * Configura o chat UDP como meio de transporte.
     */
    public void setChatUdp(Chat chat) {
        this.chatUdp = chat;
        this.useTcp = false;
    }

    /**
     * Configura o chat TCP como meio de transporte.
     */
    public void setChatTcp(ChatTCP chat) {
        this.chatTcp = chat;
        this.useTcp = true;
    }

    /**
     * Recebe uma mensagem da API de chat e processa:
     * - Mensagens de controle (REGISTER/UNREGISTER): gerencia peers
     * - Mensagens normais: replica para outros peers e exibe na view
     *
     * @param fullMessage mensagem no formato "[ip:porta] conteúdo"
     */
    @Override
    public void receiveMessage(String fullMessage) {
        String senderIp = null;
        int senderPort = 0;
        String body = fullMessage;

        // Tentar extrair o cabeçalho [ip:porta]
        if (fullMessage.startsWith("[")) {
            int closeBracket = fullMessage.indexOf("] ");
            if (closeBracket > 0) {
                String header = fullMessage.substring(1, closeBracket);
                int colonIdx = header.lastIndexOf(':');
                if (colonIdx > 0) {
                    try {
                        senderPort = Integer.parseInt(header.substring(colonIdx + 1));
                        senderIp = header.substring(0, colonIdx);
                        body = fullMessage.substring(closeBracket + 2);
                    } catch (NumberFormatException e) {
                        // Não é um cabeçalho ip:porta válido (ex: [ERRO])
                        senderIp = null;
                    }
                }
            }
        }

        // Processar mensagens de controle (somente se o remetente foi identificado)
        if (senderIp != null) {
            if (body.startsWith(REGISTER_PREFIX)) {
                handleRegister(senderIp, body);
                return;
            }
            if (body.startsWith(UNREGISTER_PREFIX)) {
                handleUnregister(senderIp, body);
                return;
            }

            // Auto-registro: se o remetente não é um peer conhecido, registrá-lo automaticamente.
            // Isso permite que qualquer aplicação baseada na mesma API do Chat entre no grupo
            // sem precisar enviar ##REGISTER## (interoperabilidade com outras duplas).
            autoRegisterIfNeeded(senderIp, senderPort);

            // Mensagem normal: replicar para todos os peers exceto o remetente
            broadcastToOthers(fullMessage, senderIp, senderPort);
        }

        // Exibir na view do host
        viewContainer.receiveMessage(fullMessage);
    }

    /**
     * Broadcast de mensagem enviada pelo próprio host para todos os peers.
     *
     * @param message texto da mensagem do host
     */
    public void broadcastFromHost(String message) {
        for (Peer peer : peers) {
            try {
                sendToPeer(peer, "[HOST] " + message);
            } catch (IOException e) {
                viewContainer.receiveMessage("[ERRO] Falha ao enviar para " + peer + ": " + e.getMessage());
            }
        }
    }

    /**
     * Retorna a quantidade de peers registrados.
     */
    public int getPeerCount() {
        return peers.size();
    }

    /**
     * Limpa todos os peers registrados.
     */
    public void close() {
        peers.clear();
    }

    // ── Métodos internos ──────────────────────────────────────────────────

    /**
     * Registra automaticamente um peer desconhecido ao receber sua primeira mensagem.
     * Permite interoperabilidade: qualquer app baseada na mesma API do Chat entra no grupo
     * sem precisar enviar mensagens de controle (##REGISTER##).
     */
    private void autoRegisterIfNeeded(String ip, int port) {
        Peer peer = new Peer(ip, port);
        if (!peers.contains(peer)) {
            peers.add(peer);
            viewContainer.receiveMessage(
                    "[SISTEMA] " + peer + " entrou automaticamente no grupo. (" + peers.size() + " peers)");
        }
    }

    private void handleRegister(String senderIp, String body) {
        String portStr = body.substring(REGISTER_PREFIX.length()).trim();
        try {
            int listeningPort = Integer.parseInt(portStr);
            Peer peer = new Peer(senderIp, listeningPort);
            if (!peers.contains(peer)) {
                peers.add(peer);
                viewContainer.receiveMessage(
                        "[SISTEMA] " + peer + " entrou no grupo. (" + peers.size() + " peers)");
            }
        } catch (NumberFormatException ignored) {
            viewContainer.receiveMessage("[ERRO] Mensagem de registro inválida: " + body);
        }
    }

    private void handleUnregister(String senderIp, String body) {
        String portStr = body.substring(UNREGISTER_PREFIX.length()).trim();
        try {
            int listeningPort = Integer.parseInt(portStr);
            Peer peer = new Peer(senderIp, listeningPort);
            if (peers.remove(peer)) {
                viewContainer.receiveMessage(
                        "[SISTEMA] " + peer + " saiu do grupo. (" + peers.size() + " peers)");
            }
        } catch (NumberFormatException ignored) {
            viewContainer.receiveMessage("[ERRO] Mensagem de saída inválida: " + body);
        }
    }

    /**
     * Replica uma mensagem para todos os peers exceto o remetente.
     */
    private void broadcastToOthers(String fullMessage, String senderIp, int senderPort) {
        for (Peer peer : peers) {
            if (isSenderPeer(peer, senderIp, senderPort)) continue;
            try {
                sendToPeer(peer, fullMessage);
            } catch (IOException e) {
                viewContainer.receiveMessage("[ERRO] Falha ao replicar para " + peer + ": " + e.getMessage());
            }
        }
    }

    /**
     * Verifica se um peer é o remetente da mensagem.
     * Para UDP: porta do remetente == porta registrada (mesmo DatagramSocket).
     * Para TCP: porta do remetente é efêmera, então comparamos apenas por IP.
     *           Funciona corretamente quando os clientes estão em máquinas diferentes.
     */
    private boolean isSenderPeer(Peer peer, String senderIp, int senderPort) {
        if (!peer.ip.equals(senderIp)) return false;
        // Correspondência exata (funciona para UDP)
        if (peer.port == senderPort) return true;
        // Para TCP, a porta do remetente é efêmera — comparar apenas por IP
        return useTcp;
    }

    /**
     * Envia uma mensagem para um peer usando o protocolo configurado (UDP ou TCP).
     */
    private void sendToPeer(Peer peer, String message) throws IOException {
        if (useTcp && chatTcp != null) {
            chatTcp.sendMessage(message, peer.ip, peer.port);
        } else if (chatUdp != null) {
            chatUdp.sendMessage(message, peer.ip, peer.port);
        }
    }
}
