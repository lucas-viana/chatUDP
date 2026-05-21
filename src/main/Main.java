package main;

import view.ChatView;

import javax.swing.*;

/**
 * Ponto de entrada da aplicação Chat UDP.
 */
public class Main {

    public static void main(String[] args) {
        // Configura o look and feel para um visual mais moderno
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        // Inicia a interface gráfica na thread do Swing (EDT)
        SwingUtilities.invokeLater(ChatView::new);
    }
}
