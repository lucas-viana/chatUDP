package chat;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GroupChatHost implements MessageContainer {

    private static final String REGISTER_PREFIX = "##REGISTER##";
    private static final String UNREGISTER_PREFIX = "##UNREGISTER##";

    private final MessageContainer viewContainer;
    private boolean useTcp;
    private final List<Peer> peers = new CopyOnWriteArrayList<>();

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

    public GroupChatHost(MessageContainer viewContainer, boolean useTcp) {
        this.viewContainer = viewContainer;
        this.useTcp = useTcp;
    }

    @Override
    public void newMessage(String fullMessage) {
        String senderIp = null;
        int senderPort = 0;
        String body = fullMessage;

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
                        senderIp = null;
                    }
                }
            }
        }

        if (senderIp != null) {
            if (body.startsWith(REGISTER_PREFIX)) {
                handleRegister(senderIp, body);
                return;
            }
            if (body.startsWith(UNREGISTER_PREFIX)) {
                handleUnregister(senderIp, body);
                return;
            }

            // O Auto-registro foi removido porque o Sender agora usa porta efêmera.
            // Precisamos do registro manual via ##REGISTER## para saber qual é a porta correta do Receiver do cliente.
            
            broadcastToOthers(fullMessage, senderIp);
        }

        viewContainer.newMessage(fullMessage);
    }

    public void broadcastFromHost(String message) {
        for (Peer peer : peers) {
            try {
                sendToPeer(peer, "[HOST] " + message);
            } catch (ChatException e) {
                viewContainer.newMessage("[ERRO] Falha ao enviar para " + peer + ": " + e.getMessage());
            }
        }
    }

    public int getPeerCount() {
        return peers.size();
    }

    public void close() {
        peers.clear();
    }

    private void handleRegister(String senderIp, String body) {
        String portStr = body.substring(REGISTER_PREFIX.length()).trim();
        try {
            int listeningPort = Integer.parseInt(portStr);
            Peer peer = new Peer(senderIp, listeningPort);
            if (!peers.contains(peer)) {
                peers.add(peer);
                viewContainer.newMessage(
                        "[SISTEMA] " + peer + " entrou no grupo. (" + peers.size() + " peers)");
            }
        } catch (NumberFormatException ignored) {
            viewContainer.newMessage("[ERRO] Mensagem de registro inválida: " + body);
        }
    }

    private void handleUnregister(String senderIp, String body) {
        String portStr = body.substring(UNREGISTER_PREFIX.length()).trim();
        try {
            int listeningPort = Integer.parseInt(portStr);
            Peer peer = new Peer(senderIp, listeningPort);
            if (peers.remove(peer)) {
                viewContainer.newMessage(
                        "[SISTEMA] " + peer + " saiu do grupo. (" + peers.size() + " peers)");
            }
        } catch (NumberFormatException ignored) {
            viewContainer.newMessage("[ERRO] Mensagem de saída inválida: " + body);
        }
    }

    private void broadcastToOthers(String fullMessage, String senderIp) {
        for (Peer peer : peers) {
            // Se o IP do peer for igual ao IP remetente, não reenvia.
            // Como as portas mudam devido aos sockets efêmeros da ChatFactory, IP é o que temos de mais seguro.
            if (peer.ip.equals(senderIp)) continue;
            try {
                sendToPeer(peer, fullMessage);
            } catch (ChatException e) {
                viewContainer.newMessage("[ERRO] Falha ao replicar para " + peer + ": " + e.getMessage());
            }
        }
    }

    private void sendToPeer(Peer peer, String message) throws ChatException {
        try {
            InetAddress address = InetAddress.getByName(peer.ip);
            Sender sender;
            if (useTcp) {
                sender = new TCPSender(address, peer.port);
            } else {
                sender = new UDPSender(address, peer.port);
            }
            sender.send(message);
        } catch (Exception e) {
            throw new ChatException("Falha na criação do Sender para " + peer, e);
        }
    }
}
