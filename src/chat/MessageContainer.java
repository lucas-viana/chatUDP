package chat;

/**
 * Interface que define o contrato para recebimento de mensagens do Chat UDP.
 * A classe de view deve implementar esta interface para receber mensagens.
 */
public interface MessageContainer {

    /**
     * Método chamado pela API do Chat quando uma mensagem é recebida.
     *
     * @param message A mensagem recebida.
     */
    void receiveMessage(String message);
}
