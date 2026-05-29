package view;

import chat.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ChatView extends JFrame implements MessageContainer {

    private static final Color BG_DARK       = new Color(18, 18, 28);
    private static final Color BG_PANEL      = new Color(28, 28, 42);
    private static final Color BG_FIELD      = new Color(38, 38, 58);
    private static final Color BG_SENT       = new Color(79, 70, 229);
    private static final Color BG_RECEIVED   = new Color(45, 45, 68);
    private static final Color ACCENT        = new Color(139, 92, 246);
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

    private JTextField fieldLocalPort;
    private JTextField fieldRemoteIp;
    private JTextField fieldRemotePort;
    private JTextPane  chatArea;
    private JTextField fieldMessage;
    private JButton    btnSend;
    private JButton    btnConnect;
    private JButton    btnDisconnect;
    private JLabel     lblStatus;

    private JRadioButton radioUdp;
    private JRadioButton radioTcp;
    private JCheckBox    chkGroupChat;
    private JCheckBox    chkIsHost;

    private Sender         sender;
    private GroupChatHost  groupHost;
    private boolean        connected = false;
    private boolean        useTcp    = false;

    private final SimpleAttributeSet attrSent     = new SimpleAttributeSet();
    private final SimpleAttributeSet attrReceived = new SimpleAttributeSet();
    private final SimpleAttributeSet attrSystem   = new SimpleAttributeSet();
    private final SimpleAttributeSet attrTime     = new SimpleAttributeSet();

    public ChatView() {
        super("Chat UDP/TCP");
        configureAttributes();
        buildUI();
        setVisible(true);
    }

    private void configureAttributes() {
        StyleConstants.setForeground(attrSent, new Color(200, 190, 255));
        StyleConstants.setFontSize(attrSent, 13);
        StyleConstants.setFontFamily(attrSent, "Segoe UI");

        StyleConstants.setForeground(attrReceived, TEXT_PRIMARY);
        StyleConstants.setFontSize(attrReceived, 13);
        StyleConstants.setFontFamily(attrReceived, "Segoe UI");

        StyleConstants.setForeground(attrSystem, TEXT_SYSTEM);
        StyleConstants.setItalic(attrSystem, true);
        StyleConstants.setFontSize(attrSystem, 12);
        StyleConstants.setFontFamily(attrSystem, "Segoe UI");

        StyleConstants.setForeground(attrTime, TEXT_SECONDARY);
        StyleConstants.setFontSize(attrTime, 11);
        StyleConstants.setFontFamily(attrTime, "Segoe UI");
    }

    private void buildUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(780, 620));
        setMinimumSize(new Dimension(620, 500));
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(),     BorderLayout.NORTH);
        add(buildChatPanel(),  BorderLayout.CENTER);
        add(buildInputPanel(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setBackground(BG_PANEL);

        JLabel title = new JLabel("💬  Chat UDP/TCP");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        lblStatus = new JLabel("● Desconectado");
        lblStatus.setFont(FONT_STATUS);
        lblStatus.setForeground(STATUS_ERR);
        lblStatus.setHorizontalAlignment(SwingConstants.RIGHT);

        topRow.add(title,     BorderLayout.WEST);
        topRow.add(lblStatus, BorderLayout.EAST);

        JPanel connPanel = buildConnectionPanel();

        JPanel headerContent = new JPanel();
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.setBackground(BG_PANEL);
        headerContent.add(topRow);
        headerContent.add(Box.createVerticalStrut(10));
        headerContent.add(connPanel);

        header.add(headerContent, BorderLayout.CENTER);

        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(14, 20, 14, 20)
        ));

        return header;
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row1.setBackground(BG_PANEL);

        fieldLocalPort  = styledTextField(5, "ex: 5000");
        fieldRemoteIp   = styledTextField(10, "ex: 192.168.1.5");
        fieldRemotePort = styledTextField(5, "ex: 5001");

        radioUdp = new JRadioButton("UDP", true);
        radioTcp = new JRadioButton("TCP");
        styleRadioButton(radioUdp);
        styleRadioButton(radioTcp);
        ButtonGroup protocolGroup = new ButtonGroup();
        protocolGroup.add(radioUdp);
        protocolGroup.add(radioTcp);

        row1.add(styledLabel("Porta Local:"));
        row1.add(fieldLocalPort);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(styledLabel("IP Remoto:"));
        row1.add(fieldRemoteIp);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(styledLabel("Porta Remota:"));
        row1.add(fieldRemotePort);
        row1.add(Box.createHorizontalStrut(10));
        row1.add(styledLabel("Protocolo:"));
        row1.add(radioUdp);
        row1.add(radioTcp);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setBackground(BG_PANEL);

        chkGroupChat = new JCheckBox("Chat em Grupo");
        chkIsHost    = new JCheckBox("Sou o Host");
        styleCheckBox(chkGroupChat);
        styleCheckBox(chkIsHost);
        chkIsHost.setEnabled(false);

        chkGroupChat.addActionListener(e -> {
            chkIsHost.setEnabled(chkGroupChat.isSelected());
            if (!chkGroupChat.isSelected()) {
                chkIsHost.setSelected(false);
            }
        });

        btnConnect    = buildButton("Conectar",    ACCENT,          new Color(109, 40, 217));
        btnDisconnect = buildButton("Desconectar", new Color(60, 60, 90), new Color(80, 80, 110));
        btnDisconnect.setEnabled(false);

        btnConnect.addActionListener(e -> handleConnect());
        btnDisconnect.addActionListener(e -> handleDisconnect());

        row2.add(chkGroupChat);
        row2.add(chkIsHost);
        row2.add(Box.createHorizontalStrut(16));
        row2.add(btnConnect);
        row2.add(btnDisconnect);

        panel.add(row1);
        panel.add(Box.createVerticalStrut(8));
        panel.add(row2);

        return panel;
    }

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

        scroll.getVerticalScrollBar().setBackground(BG_PANEL);
        scroll.getVerticalScrollBar().setForeground(BORDER_COLOR);

        appendSystemMessage("Bem-vindo ao Chat UDP/TCP! Configure a conexão e clique em Conectar.");

        return scroll;
    }

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
        fieldMessage.setToolTipText("Digite sua mensagem aqui...");

        fieldMessage.addActionListener(e -> handleSend());

        btnSend = buildButton("Enviar  ➤", ACCENT, new Color(109, 40, 217));
        btnSend.setEnabled(false);
        btnSend.addActionListener(e -> handleSend());
        btnSend.setPreferredSize(new Dimension(120, 38));

        panel.add(fieldMessage, BorderLayout.CENTER);
        panel.add(btnSend,      BorderLayout.EAST);

        return panel;
    }

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

        boolean isGroupClient = chkGroupChat.isSelected() && !chkIsHost.isSelected();
        boolean isHost = chkGroupChat.isSelected() && chkIsHost.isSelected();
        
        // Se for host de grupo puro (que só vai replicar), talvez nem saiba quem é o remoto ainda
        // No entanto, ChatFactory precisa de um IP/Porta remoto para instanciar o Sender base!
        // No caso do host do grupo sem IP, colocaremos 127.0.0.1 porta 1 provisoriamente,
        // mas ele usa o GroupChatHost para enviar para seus peers separadamente.
        if (remoteIp.isEmpty()) {
            if (isHost) {
                remoteIp = "127.0.0.1";
            } else {
                showError("Informe o IP remoto!");
                return;
            }
        }
        if (remotePortStr.isEmpty()) {
            if (isHost) {
                remotePortStr = "1";
            } else {
                showError("Informe a porta remota!");
                return;
            }
        }
        
        int remotePort;
        try {
            remotePort = Integer.parseInt(remotePortStr);
            if (remotePort < 1 || remotePort > 65535) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showError("Porta remota inválida! Use um número entre 1 e 65535.");
            return;
        }

        useTcp = radioTcp.isSelected();
        boolean isGroupMode = chkGroupChat.isSelected();

        try {
            cleanupResources();

            if (isGroupMode && isHost) {
                groupHost = new GroupChatHost(this, useTcp);
                sender = ChatFactory.build(useTcp, remoteIp, remotePort, localPort, groupHost);
                appendSystemMessage("🏠 Modo Host ativado. Aguardando clientes...");
            } else {
                sender = ChatFactory.build(useTcp, remoteIp, remotePort, localPort, this);
            }

            connected = true;
            setConnectionState(true);

            String protocol = useTcp ? "TCP" : "UDP";
            appendSystemMessage("✅ Conectado na porta local " + localPort + " (" + protocol + ").");

            if (!remoteIp.equals("127.0.0.1") || !isHost) {
                appendSystemMessage("📡 Pronto para enviar para " + remoteIp + ":" + remotePortStr + ".");
            }

            if (isGroupMode && !isHost) {
                sendControlMessage("##REGISTER##" + localPort);
                appendSystemMessage("📋 Registro enviado ao host.");
            }

        } catch (ChatException ex) {
            showError("Erro ao conectar: " + ex.getMessage());
            appendSystemMessage("❌ Falha na conexão: " + ex.getMessage());
        }
    }

    private void handleDisconnect() {
        if (chkGroupChat.isSelected() && !chkIsHost.isSelected()) {
            sendControlMessage("##UNREGISTER##" + fieldLocalPort.getText().trim());
            appendSystemMessage("📋 Saída do grupo notificada ao host.");
        }

        cleanupResources();
        connected = false;
        setConnectionState(false);
        appendSystemMessage("🔴 Desconectado.");
    }

    private void handleSend() {
        if (!connected) {
            showError("Você não está conectado!");
            return;
        }

        String message = fieldMessage.getText().trim();
        if (message.isEmpty()) return;

        boolean isHost = chkGroupChat.isSelected() && chkIsHost.isSelected();
        if (isHost && groupHost != null) {
            groupHost.broadcastFromHost(message);
            appendSentMessage("Você [HOST]", message);
            fieldMessage.setText("");
            return;
        }

        try {
            if (sender != null) {
                sender.send(message);
            }
            appendSentMessage("Você", message);
            fieldMessage.setText("");
        } catch (ChatException ex) {
            showError("Erro ao enviar mensagem: " + ex.getMessage());
            appendSystemMessage("❌ Falha ao enviar: " + ex.getMessage());
        }
    }

    private void sendControlMessage(String controlMsg) {
        try {
            if (sender != null) {
                sender.send(controlMsg);
            }
        } catch (ChatException e) {
            appendSystemMessage("⚠ Falha ao enviar mensagem de controle: " + e.getMessage());
        }
    }

    private void cleanupResources() {
        if (groupHost != null) {
            groupHost.close();
            groupHost = null;
        }
        sender = null;
        ChatFactory.stopReceiver(); // Pára os receivers
    }

    @Override
    public void newMessage(String message) {
        SwingUtilities.invokeLater(() -> appendReceivedMessage(message));
    }

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

        radioUdp.setEnabled(!connected);
        radioTcp.setEnabled(!connected);
        chkGroupChat.setEnabled(!connected);
        chkIsHost.setEnabled(!connected && chkGroupChat.isSelected());

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

    private void styleRadioButton(JRadioButton rb) {
        rb.setFont(FONT_LABEL);
        rb.setForeground(TEXT_PRIMARY);
        rb.setBackground(BG_PANEL);
        rb.setFocusPainted(false);
        rb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleCheckBox(JCheckBox cb) {
        cb.setFont(FONT_LABEL);
        cb.setForeground(TEXT_PRIMARY);
        cb.setBackground(BG_PANEL);
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
