package chat;

public interface Receiver extends Runnable {
    void run();
    
    // Método adicionado para permitir que o socket seja fechado corretamente
    // ao clicar em "Desconectar" na interface gráfica.
    void close();
}
