import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;

/**
 * A simple P2P (Peer-to-Peer) chat application using Java Swing.
 * One user must "Host" and the other must "Join".
 */
public class P2PChatGUI {

    // GUI Components
    private JFrame frame;
    private JTextArea messageArea;
    private JTextField textField;
    private JButton sendButton;

    // Network Components
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public P2PChatGUI() {
        initializeGUI();
    }

    /**
     * Initializes the graphical user interface.
     */
    private void initializeGUI() {
        frame = new JFrame("P2P Chat");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);

        // --- Message Area (to display chat history) ---
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        // --- Input Panel (for typing and sending) ---
        JPanel southPanel = new JPanel(new BorderLayout());
        textField = new JTextField();
        sendButton = new JButton("Send");
        
        // Add action listener to the Send button
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        
        // Add action listener to the text field (to send on Enter)
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        southPanel.add(textField, BorderLayout.CENTER);
        southPanel.add(sendButton, BorderLayout.EAST);
        frame.add(southPanel, BorderLayout.SOUTH);

        // Initially, disable the chat components until connected
        setChatEnabled(false);

        frame.setVisible(true);
    }

    /**
     * Shows the initial dialog to choose between Hosting and Joining.
     */
    public void start() {
        Object[] options = {"Host a Chat", "Join a Chat"};
        int choice = JOptionPane.showOptionDialog(frame,
                "How do you want to connect?",
                "P2P Chat Setup",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            // --- HOST ---
            String portStr = JOptionPane.showInputDialog(frame, "Enter port to host on:", "Host Chat", JOptionPane.PLAIN_MESSAGE);
            if (portStr != null && !portStr.isEmpty()) {
                try {
                    int port = Integer.parseInt(portStr);
                    // Start connection logic in a new thread to not freeze the GUI
                    new Thread(() -> connectAsHost(port)).start();
                } catch (NumberFormatException e) {
                    showError("Invalid port number.");
                }
            }
        } else if (choice == JOptionPane.NO_OPTION) {
            // --- JOIN (CLIENT) ---
            showJoinDialog();
        } else {
            // User closed the dialog
            frame.dispose();
            System.exit(0);
        }
    }

    /**
     * Shows a custom dialog for the "Join" option.
     */
    private void showJoinDialog() {
        // Custom panel for multiple inputs
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField hostField = new JTextField("localhost");
        JTextField portField = new JTextField("5000");
        panel.add(new JLabel("Host IP:"));
        panel.add(hostField);
        panel.add(new JLabel("Port:"));
        panel.add(portField);

        int result = JOptionPane.showConfirmDialog(frame, panel, "Join Chat", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            try {
                String host = hostField.getText();
                int port = Integer.parseInt(portField.getText());
                // Start connection logic in a new thread
                new Thread(() -> connectAsClient(host, port)).start();
            } catch (NumberFormatException e) {
                showError("Invalid port number.");
            }
        }
    }

    /**
     * Logic for the Host. Waits for a client to connect.
     * Runs on a background thread.
     */
    private void connectAsHost(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Get local IP to display
            String localIP = InetAddress.getLocalHost().getHostAddress();
            updateMessageArea("Waiting for a peer to connect on...\nIP: " + localIP + "\nPort: " + port);
            
            this.socket = serverSocket.accept(); // Blocks here
            
            updateMessageArea("Peer connected! (" + socket.getInetAddress() + ")");
            initializeStreams();
            startMessageReader();
            setChatEnabled(true);

        } catch (IOException e) {
            showError("Host connection error: " + e.getMessage());
        }
    }

    /**
     * Logic for the Client. Connects to a host.
     * Runs on a background thread.
     */
    private void connectAsClient(String host, int port) {
        try {
            updateMessageArea("Connecting to " + host + ":" + port + "...");
            
            this.socket = new Socket(host, port); // Blocks here
            
            updateMessageArea("Connected to peer!");
            initializeStreams();
            startMessageReader();
            setChatEnabled(true);

        } catch (IOException e) {
            showError("Client connection error: " + e.getMessage());
        }
    }

    /**
     * Initializes the input and output streams once the socket is connected.
     */
    private void initializeStreams() throws IOException {
        // PrintWriter: for sending messages (true = auto-flush)
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        
        // BufferedReader: for reading incoming messages
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * Starts the background thread that continuously listens for incoming messages.
     */
    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    // Update GUI safely on the Event Dispatch Thread (EDT)
                    final String receivedMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append("Peer: " + receivedMessage + "\n");
                    });
                }
            } catch (IOException e) {
                // This usually happens when the peer disconnects
                updateMessageArea("Peer has disconnected.");
                setChatEnabled(false);
            }
        });
        readerThread.start();
    }

    /**
     * Sends the content of the text field to the peer.
     */
    private void sendMessage() {
        String message = textField.getText();
        if (message != null && !message.trim().isEmpty() && writer != null) {
            writer.println(message);
            messageArea.append("Me: " + message + "\n"); // Display your own message
            textField.setText(""); // Clear the text field
        }
    }
    
    // --- Thread-Safe GUI Helper Methods ---

    /**
     * Safely appends a message to the text area from any thread.
     * @param message The message to append.
     */
    private void updateMessageArea(String message) {
        SwingUtilities.invokeLater(() -> {
            messageArea.append(message + "\n");
        });
    }

    /**
     * Safely enables or disables the chat input components.
     * @param enabled true to enable, false to disable.
     */
    private void setChatEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            textField.setEnabled(enabled);
            sendButton.setEnabled(enabled);
            if(enabled) {
                textField.requestFocus();
            }
        });
    }

    /**
     * Safely shows an error message dialog.
     * @param message The error message.
     */
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }


    /**
     * Main method to start the application.
     */
    public static void main(String[] args) {
        // Run the GUI creation and logic on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            P2PChatGUI chat = new P2PChatGUI();
            chat.start();
        });
    }
}
