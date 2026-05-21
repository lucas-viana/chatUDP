package view;

import chat.Chat;
import chat.MessageContainer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Interface gráfica do Chat UDP usando Java Swing.
 * Implementa MessageContainer para receber mensagens da API do Chat.
 */
public class ChatView extends JFrame implements MessageContainer {

    // ── Cores do tema ──────────────────────────────────────────────────────
    private static final Color BG_DARK       = new Color(18, 18, 28);
    private static final Color BG_PANEL      = new Color(28, 28, 42);
    private static final Color BG_FIELD      = new Color(38, 38, 58);
    private static final Color BG_SENT       = new Color(79, 70, 229);   // índigo
    private static final Color BG_RECEIVED   = new Color(45, 45, 68);
    private static final Color ACCENT        = new Color(139, 92, 246);  // violeta
    private static final Color ACCENT_HOVER  = new Color(167, 139, 250);
    private static final Color TEXT_PRIMARY  = new Color(240, 240, 255);
    private static final Color TEXT_SECONDARY = new Color(160, 160, 200);
    private static final Color TEXT_SYSTEM   = new Color(100, 200, 140);
    private static final Color BORDER_COLOR  = new Color(60, 60, 90);
    private static final Color STATUS_OK     = new Color(34, 197, 94);
    private static final Color STATUS_ERR    = new Color(239, 68, 68);

    private static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL   = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font FONT_INPUT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_CHAT    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BTN     = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_STATUS  = new Font("Segoe UI", Font.ITALIC, 11);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Componentes ────────────────────────────────────────────────────────
    private JTextField fieldLocalPort;
    private JTextField fieldRemoteIp;
    private JTextField fieldRemotePort;
    private JTextPane  chatArea;
    private JTextField fieldMessage;
    private JButton    btnSend;
    private JButton    btnConnect;
    private JButton    btnDisconnect;
    private JLabel     lblStatus;

    // ── Lógica ─────────────────────────────────────────────────────────────
    private Chat chat;
    private boolean connected = false;

    // ── Atributos de estilo para o painel de mensagens ─────────────────────
    private final SimpleAttributeSet attrSent     = new SimpleAttributeSet();
    private final SimpleAttributeSet attrReceived = new SimpleAttributeSet();
    private final SimpleAttributeSet attrSystem   = new SimpleAttributeSet();
    private final SimpleAttributeSet attrTime     = new SimpleAttributeSet();

    // ======================================================================
    public ChatView() {
        super("Chat UDP");
        configureAttributes();
        buildUI();
        setVisible(true);
    }

    // ── Configuração dos atributos de texto ────────────────────────────────
    private void configureAttributes() {
        // Enviados
        StyleConstants.setForeground(attrSent, new Color(200, 190, 255));
        StyleConstants.setFontSize(attrSent, 13);
        StyleConstants.setFontFamily(attrSent, "Segoe UI");

        // Recebidos
        StyleConstants.setForeground(attrReceived, TEXT_PRIMARY);
        StyleConstants.setFontSize(attrReceived, 13);
        StyleConstants.setFontFamily(attrReceived, "Segoe UI");

        // Sistema
        StyleConstants.setForeground(attrSystem, TEXT_SYSTEM);
        StyleConstants.setItalic(attrSystem, true);
        StyleConstants.setFontSize(attrSystem, 12);
        StyleConstants.setFontFamily(attrSystem, "Segoe UI");

        // Hora
        StyleConstants.setForeground(attrTime, TEXT_SECONDARY);
        StyleConstants.setFontSize(attrTime, 11);
        StyleConstants.setFontFamily(attrTime, "Segoe UI");
    }

    // ── Construção da interface ────────────────────────────────────────────
    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(780, 600));
        setMinimumSize(new Dimension(600, 480));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildChatPanel(),  BorderLayout.CENTER);
        add(buildInputPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        // Ação de fechar janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (chat != null) chat.close();
            }
        });
    }

    // ── Cabeçalho ──────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(16, 20, 16, 20));

        // Título
        JLabel title = new JLabel("💬  Chat UDP");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        // Status
        lblStatus = new JLabel("● Desconectado");
        lblStatus.setFont(FONT_STATUS);
        lblStatus.setForeground(STATUS_ERR);
        lblStatus.setHorizontalAlignment(SwingConstants.RIGHT);

        // Painel de configuração de conexão
        JPanel connPanel = buildConnectionPanel();

        header.add(title,      BorderLayout.WEST);
        header.add(connPanel,  BorderLayout.CENTER);
        header.add(lblStatus,  BorderLayout.EAST);

        // Borda inferior
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 20, 14, 20)
        ));

        return header;
    }

    // ── Painel de conexão ──────────────────────────────────────────────────
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        panel.setBackground(BG_PANEL);

        // Porta local
        JLabel lblLocal = styledLabel("Porta Local:");
        fieldLocalPort  = styledTextField(6, "ex: 5000");

        // IP remoto
        JLabel lblIp   = styledLabel("IP Remoto:");
        fieldRemoteIp  = styledTextField(12, "ex: 192.168.1.5");

        // Porta remota
        JLabel lblRPort  = styledLabel("Porta Remota:");
        fieldRemotePort  = styledTextField(6, "ex: 5001");

        // Botões
        btnConnect    = buildButton("Conectar",    ACCENT,          new Color(109, 40, 217));
        btnDisconnect = buildButton("Desconectar", new Color(60, 60, 90), new Color(80, 80, 110));
        btnDisconnect.setEnabled(false);

        btnConnect.addActionListener(e -> handleConnect());
        btnDisconnect.addActionListener(e -> handleDisconnect());

        panel.add(lblLocal);
        panel.add(fieldLocalPort);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(lblIp);
        panel.add(fieldRemoteIp);
        panel.add(Box.createHorizontalStrut(6));
        panel.add(lblRPort);
        panel.add(fieldRemotePort);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(btnConnect);
        panel.add(btnDisconnect);

        return panel;
    }

    // ── Painel de chat ─────────────────────────────────────────────────────
    private JScrollPane buildChatPanel() {
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_DARK);
        chatArea.setForeground(TEXT_PRIMARY);
        chatArea.setFont(FONT_CHAT);
        chatArea.setBorder(new EmptyBorder(12, 16, 12, 16));

        JScrollPane scroll = new JScrollPane(chatArea);
        scroll.setBackground(BG_DARK);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_DARK);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Personaliza a scrollbar
        scroll.getVerticalScrollBar().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setForeground(BORDER_COLOR);

        // Mensagem inicial
        appendSystemMessage("Bem-vindo ao Chat UDP! Configure a conexão e clique em Conectar.");

        return scroll;
    }

    // ── Painel de entrada ──────────────────────────────────────────────────
    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBackground(BG_PANEL);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(12, 16, 12, 16)
        ));

        fieldMessage = new JTextField();
        fieldMessage.setBackground(BG_FIELD);
        fieldMessage.setForeground(TEXT_PRIMARY);
        fieldMessage.setCaretColor(ACCENT_HOVER);
        fieldMessage.setFont(FONT_INPUT);
        fieldMessage.setEnabled(false);
        fieldMessage.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));
        // Placeholder hint
        fieldMessage.setToolTipText("Digite sua mensagem aqui...");

        // Enviar ao pressionar Enter
        fieldMessage.addActionListener(e -> handleSend());

        btnSend = buildButton("Enviar  ➤", ACCENT, new Color(109, 40, 217));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> handleSend());
        btnSend.setPreferredSize(new Dimension(120, 38));

        panel.add(fieldMessage, BorderLayout.CENTER);
        panel.add(btnSend,      BorderLayout.EAST);

        return panel;
    }

    // ── Ações ──────────────────────────────────────────────────────────────
    private void handleConnect() {
        String localPortStr  = fieldLocalPort.getText().trim();
        String remoteIp      = fieldRemoteIp.getText().trim();
        String remotePortStr = fieldRemotePort.getText().trim();

        if (localPortStr.isEmpty()) {
            showError("Informe a porta local!");
            return;
        }

        int localPort;
        try {
            localPort = Integer.parseInt(localPortStr);
            if (localPort < 1 || localPort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Porta local inválida! Use um número entre 1 e 65535.");
            return;
        }

        if (!remoteIp.isEmpty() || !remotePortStr.isEmpty()) {
            if (remoteIp.isEmpty()) {
                showError("Informe o IP remoto!");
                return;
            }
            if (remotePortStr.isEmpty()) {
                showError("Informe a porta remota!");
                return;
            }
            try {
                int rp = Integer.parseInt(remotePortStr);
                if (rp < 1 || rp > 65535) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                showError("Porta remota inválida! Use um número entre 1 e 65535.");
                return;
            }
        }

        try {
            if (chat != null) chat.close();
            chat = new Chat(localPort, this);
            connected = true;

            // Atualizar UI
            setConnectionState(true);
            appendSystemMessage("✅ Conectado na porta local " + localPort + ".");
            if (!remoteIp.isEmpty()) {
                appendSystemMessage("📡 Pronto para enviar para " + remoteIp + ":" + remotePortStr + ".");
            }

        } catch (IOException ex) {
            showError("Erro ao conectar: " + ex.getMessage());
            appendSystemMessage("❌ Falha na conexão: " + ex.getMessage());
        }
    }

    private void handleDisconnect() {
        if (chat != null) {
            chat.close();
            chat = null;
        }
        connected = false;
        setConnectionState(false);
        appendSystemMessage("🔴 Desconectado.");
    }

    private void handleSend() {
        if (!connected || chat == null) {
            showError("Você não está conectado!");
            return;
        }

        String message   = fieldMessage.getText().trim();
        String remoteIp  = fieldRemoteIp.getText().trim();
        String remotePortStr = fieldRemotePort.getText().trim();

        if (message.isEmpty()) return;

        if (remoteIp.isEmpty() || remotePortStr.isEmpty()) {
            showError("Informe o IP e a porta remota para enviar mensagens!");
            return;
        }

        int remotePort;
        try {
            remotePort = Integer.parseInt(remotePortStr);
        } catch (NumberFormatException ex) {
            showError("Porta remota inválida!");
            return;
        }

        try {
            chat.sendMessage(message, remoteIp, remotePort);
            appendSentMessage("Você", message);
            fieldMessage.setText("");
        } catch (IOException ex) {
            showError("Erro ao enviar mensagem: " + ex.getMessage());
            appendSystemMessage("❌ Falha ao enviar: " + ex.getMessage());
        }
    }

    // ── MessageContainer ───────────────────────────────────────────────────
    @Override
    public void receiveMessage(String message) {
        // Atualizar UI na thread do Swing
        SwingUtilities.invokeLater(() -> appendReceivedMessage(message));
    }

    // ── Helpers de UI ──────────────────────────────────────────────────────
    private void setConnectionState(boolean connected) {
        if (connected) {
            lblStatus.setText("● Conectado");
            lblStatus.setForeground(STATUS_OK);
        } else {
            lblStatus.setText("● Desconectado");
            lblStatus.setForeground(STATUS_ERR);
        }
        btnConnect.setEnabled(!connected);
        btnDisconnect.setEnabled(connected);
        fieldLocalPort.setEnabled(!connected);
        fieldMessage.setEnabled(connected);
        btnSend.setEnabled(connected);
        if (connected) fieldMessage.requestFocus();
    }

    private void appendSentMessage(String sender, String message) {
        String time = LocalTime.now().format(TIME_FMT);
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "\n", attrSent);
            doc.insertString(doc.getLength(), "  [" + time + "] ", attrTime);
            doc.insertString(doc.getLength(), "➤ " + sender + ": ", attrSent);
            doc.insertString(doc.getLength(), message + "\n", attrSent);
        } catch (BadLocationException ignored) {}
        scrollToBottom();
    }

    private void appendReceivedMessage(String message) {
        String time = LocalTime.now().format(TIME_FMT);
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "\n", attrReceived);
            doc.insertString(doc.getLength(), "  [" + time + "] ", attrTime);
            doc.insertString(doc.getLength(), "◀ " + message + "\n", attrReceived);
        } catch (BadLocationException ignored) {}
        scrollToBottom();
    }

    private void appendSystemMessage(String message) {
        String time = LocalTime.now().format(TIME_FMT);
        StyledDocument doc = chatArea.getStyledDocument();
        try {
            doc.insertString(doc.getLength(), "  [" + time + "] ", attrTime);
            doc.insertString(doc.getLength(), message + "\n", attrSystem);
        } catch (BadLocationException ignored) {}
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ── Factory helpers ────────────────────────────────────────────────────
    private JLabel styledLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SECONDARY);
        return lbl;
    }

    private JTextField styledTextField(int cols, String tooltip) {
        JTextField tf = new JTextField(cols);
        tf.setBackground(BG_FIELD);
        tf.setForeground(TEXT_PRIMARY);
        tf.setCaretColor(ACCENT_HOVER);
        tf.setFont(FONT_INPUT);
        tf.setToolTipText(tooltip);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(5, 8, 5, 8)
        ));
        return tf;
    }

    private JButton buildButton(String text, Color bg, Color hoverBg) {
        JButton btn = new JButton(text) {
            private Color currentBg = bg;

            {
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseEntered(MouseEvent e) {
                        if (isEnabled()) { currentBg = hoverBg; repaint(); }
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        currentBg = bg; repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled() ? currentBg : new Color(50, 50, 70));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_BTN);
        btn.setForeground(TEXT_PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        return btn;
    }
}
