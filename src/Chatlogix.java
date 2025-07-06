import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.mysql.cj.xdevapi.Client;

public class Chatlogix {
	private static final String DB_URL = EnvLoader.get("DB_URL");
	private static final String DB_USER = EnvLoader.get("DB_USER");
	private static final String DB_PASSWORD = EnvLoader.get("DB_PASSWORD");


    // Login Frame
    static class LoginFrame extends JFrame {
        JTextField usernameField;
        JPasswordField passwordField;
        static String loggedInUsername = "";
        public LoginFrame() {
            setTitle("Login ");
            setBounds(0, 0, 2000, 2000);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(null);

            // Create a custom panel with a gradient
            GradientPanel loginPanel = new GradientPanel();

            // Set the layout and add components
            loginPanel.setLayout(null);

            JLabel usernameLabel = new JLabel("Username:");
            usernameLabel.setFont(new Font("Arial", Font.PLAIN, 22)); // Updated font size to 22
            usernameLabel.setBounds(530, 250, 200, 30);
            loginPanel.add(usernameLabel);

            usernameField = new JTextField();
            usernameField.setBounds(670, 250, 200, 35);
            usernameField.setFont(new Font("Arial", Font.PLAIN, 24)); // Set font size to 24 for text box
            loginPanel.add(usernameField);

            JLabel passwordLabel = new JLabel("Password:");
            passwordLabel.setFont(new Font("Arial", Font.PLAIN, 22)); // Updated font size to 22
            passwordLabel.setBounds(530, 310, 200, 30);
            loginPanel.add(passwordLabel);

            passwordField = new JPasswordField();
            passwordField.setBounds(670, 310, 200, 35);
            passwordField.setFont(new Font("Arial", Font.PLAIN, 24)); // Set font size to 24 for text box
            loginPanel.add(passwordField);

            JButton loginButton = new JButton("Login");
            loginButton.setBounds(660, 380, 100, 30);
            loginButton.addActionListener(e -> validateCredentials());
            loginPanel.add(loginButton);

            JButton exitButton = new JButton("Exit");
            exitButton.setBounds(780, 380, 100, 30);
            exitButton.addActionListener(e -> System.exit(0));
            loginPanel.add(exitButton);

            // Set the gradient panel as the frame content pane
            setContentPane(loginPanel);

            setVisible(true);
}

        private void validateCredentials() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ? AND password = ?")) {
                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(this, "Login Successful!");
                    loggedInUsername = username;
                    // Close the Login Frame
                    dispose();

                    // Launch Chatlogix
                    SwingUtilities.invokeLater(() -> {
                        JFrame frame = new JFrame("Chatlogix");
                        frame.setBounds(0, 0, 2000, 2000);

                        Chatlogix outer = new Chatlogix();
                        Chatlogix.MyApplet applet = outer.new MyApplet();

                        applet.init();
                        applet.start();
                        frame.add(applet);

                        frame.setVisible(true);
                        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
                    });
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid Username or Password", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database Connection Error!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Custom panel for gradient background
        class GradientPanel extends JPanel {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                Color color1 = new Color(Integer.parseInt("FF7E5F", 16));
                Color color2 = new Color(Integer.parseInt("FEB47B", 16));
                LinearGradientPaint gradient = new LinearGradientPaint(
                        0, 0, getWidth(), getHeight(),                // Start (x1, y1) and End (x2, y2)
                        new float[]{0f, 1f},                         // Color distribution
                        new Color[]{color1, color2}                  // Colors in the gradient
                );

                // Apply gradient to the Graphics2D object
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill the background with gradient
            }
        }

        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> new LoginFrame());
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame());
    }

    private static String generateAlphanumericString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            result.append(chars.charAt(randomIndex));
        }
        return result.toString();
    }

	    public class MyApplet extends JApplet {
	        CardLayout cardLayout;
	        static JPanel cardPanel;
	        Thread serverThread;
	        public static Client newClient;
	        public void terminateSession() {
	            // Save chat to file and database before terminating
	            saveChatToFile();
	            saveChatToDatabase();
	            JOptionPane.showMessageDialog(null, "Ending session...");
	            System.exit(0);
	        }
	       
	       
	        @Override
	        public void init() {
	            setLayout(null);
	            getContentPane().setBackground(Color.black);
	
	            cardPanel = new JPanel();
	            cardLayout = new CardLayout();
	            cardPanel.setLayout(cardLayout);
	
	            JPanel buttonPanel = new BorderPanel();  // Create the BorderPanel instance here
	             JPanel joinPanel = createJoinPanel();
	            JPanel createPanel = createNewSessionPanel();
	            JPanel chatPanel = createChatPanel();
	
	            cardPanel.add(buttonPanel, "ButtonPanel");
	            cardPanel.add(joinPanel, "Join");
	            cardPanel.add(createPanel, "Create");
	            cardPanel.add(chatPanel, "ChatPanel");
	
	            cardPanel.setBounds(10, 15, 1500, 950);
	            add(cardPanel);
	
	            cardLayout.show(cardPanel, "ButtonPanel");
	        }
	
	       

	        class BorderPanel extends JPanel {
	            Button joinButton, createButton;

	            BorderPanel() {
	                JLabel usernameLabel = new JLabel("Welcome, " + LoginFrame.loggedInUsername); 
	                usernameLabel.setFont(new Font("Arial", Font.ITALIC, 20));
	                usernameLabel.setBounds(1300, 30, 400, 30);
	                add(usernameLabel);
	                joinButton = new Button("Join");
	                createButton = new Button("Create New Session");

	                setLayout(null);

	                joinButton.setBounds(500, 120, 200, 100);
	                createButton.setBounds(800, 120, 200, 100);

	                Font buttonFont = new Font("Times New Roman", Font.BOLD, 18);
	                createButton.setFont(buttonFont);
	                joinButton.setFont(buttonFont);

	                add(joinButton);
	                add(createButton);
	                
	               
	             /*   
	                // Chat history display setup
	                chatHistoryListModel = new DefaultListModel<>();
	                chatHistoryList = new JList<>(chatHistoryListModel);
	                chatHistoryList.setBounds(500, 300, 500, 300);
	                add(chatHistoryList);

	                // Load chat history files into the list
	                loadChatHistory(chatHistoryListModel);
*/
	                // Action Listeners
	                joinButton.addActionListener(e -> cardLayout.show(cardPanel, "Join"));
	                createButton.addActionListener(e -> {
	                    cardLayout.show(cardPanel, "Create");
	                    if (serverThread == null || !serverThread.isAlive()) {
	                        serverThread = new Thread(() -> ChatServer.startServer());
	                        serverThread.start();
	                    }
	                });
	                
	            }
	               /*  Action when selecting a chat file from the list
	                chatHistoryList.addListSelectionListener(e -> {
	                    if (!e.getValueIsAdjusting()) {
	                        String selectedFile = chatHistoryList.getSelectedValue();
	                        if (selectedFile != null) {
	                            displayChatFileContent(selectedFile);
	                        }
	                    }
	                });
	            }

	            private void loadChatHistory(DefaultListModel<String> chatHistoryListModel) {
	                // Load the chat history from the database or file and populate the list model
	                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
	                     PreparedStatement stmt = conn.prepareStatement("SELECT chat_content FROM history WHERE username = ?")) {
	                    stmt.setString(1, LoginFrame.loggedInUsername); // Get history for the logged-in user
	                    ResultSet rs = stmt.executeQuery();

	                    while (rs.next()) {
	                        String chatContent = rs.getString("chat_content");
	                        chatHistoryListModel.addElement(chatContent); // Add each chat message to the list model
	                    }
	                } catch (SQLException e) {
	                    e.printStackTrace();
	                    JOptionPane.showMessageDialog(null, "Failed to load chat history!", "Error", JOptionPane.ERROR_MESSAGE);
	                }
	            }

	            private void displayChatFileContent(String fileName) {
	                // Implement this method to display the contents of the selected file
	                File selectedFile = new File("chat_logs", fileName);
	                if (selectedFile.exists()) {
	                    // Open the file and display its content (could be in a dialog or new panel)
	                    try {
	                        String content = new String(java.nio.file.Files.readAllBytes(selectedFile.toPath()));
	                        JOptionPane.showMessageDialog(this, content, "Chat History: " + fileName, JOptionPane.INFORMATION_MESSAGE);
	                    } catch (IOException e) {
	                        e.printStackTrace();
	                    }
	                }
	            }
*/
	            @Override
	            protected void paintComponent(Graphics g) {
	                super.paintComponent(g);
	                Graphics2D g2d = (Graphics2D) g;
	                Color color1 = new Color(Integer.parseInt("FF7E5F", 16));
	                Color color2 = new Color(Integer.parseInt("FEB47B", 16));
	                LinearGradientPaint gradient = new LinearGradientPaint(
	                        0, 0, getWidth(), getHeight(),
	                        new float[]{0f, 1f},
	                        new Color[]{color1, color2}
	                );

	                g2d.setPaint(gradient);
	                g2d.fillRect(0, 0, getWidth(), getHeight());
	            }
	        }


        public JPanel createJoinPanel() {
        	CustomPanel panel = new CustomPanel();
            panel.setLayout(null); // Absolute positioning
            
            Button back = new Button("BACK");
            back.setBounds(10, 20, 150, 40);
            back.setFont(new Font("Times New Roman", Font.ITALIC,18));
            
          JLabel label = new JLabel("Enter Session Code:");
            label.setFont(new Font("Times New Roman", Font.BOLD, 22));
            label.setBounds(50, 80, 250, 30);
            label.setForeground(Color.black);
            JTextField sessionCodeField = new JTextField();
            sessionCodeField.setBounds(50, 130, 300, 40);

            Button joinButton = new Button("Join");
            joinButton.setBounds(50, 200, 150, 40);
            joinButton.setFont(new Font("Times New Roman", Font.BOLD, 22));

            back.addActionListener(e->{
            	 cardLayout.show(cardPanel, "ButtonPanel");
            });
            
            joinButton.addActionListener(e -> {
                String sessionCode = sessionCodeField.getText();
                if (isSessionCodeValid(sessionCode)) {
                    cardLayout.show(cardPanel, "ChatPanel");
                    try {
                        newClient = new Client(null);
                        newClient.startConnection("localhost", 8080);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    JOptionPane.showMessageDialog(panel, "Invalid Session Code!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(back);
            panel.add(label);
            panel.add(sessionCodeField);
            panel.add(joinButton);

            return panel;
        }

        class CustomPanel extends JPanel {  // Renamed to CustomPanel for clarity
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Colors for gradient
                Color color1 = new Color(Integer.parseInt("FF7E5F", 16)); // Peach color
                Color color2 = new Color(Integer.parseInt("FEB47B", 16)); // Light orange color

                // Create linear gradient
                LinearGradientPaint gradient = new LinearGradientPaint(
                        0, 0, getWidth(), getHeight(),  // Gradient from top-left to bottom-right
                        new float[]{0f, 1f},  // Start to end of the gradient
                        new Color[]{color1, color2}  // Colors used in the gradient
                );

                // Apply the gradient to the Graphics2D object and fill the entire panel
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        private boolean isSessionCodeValid(String code) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM code WHERE id = ?")) {
                stmt.setString(1, code);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
            
        }

        public JPanel createNewSessionPanel() {
            create panel = new create();
            panel.setLayout(null);
            decoration createNewSessionPanel = new decoration();
            JLabel label = new JLabel("Session Code:");
            label.setFont(new Font("Times New Roman", Font.BOLD, 24));
            label.setBounds(50, 80, 250, 30);

            JTextField codeField = new JTextField();
            codeField.setBounds(50, 130, 300, 40);
            codeField.setFont(new Font("Times New Roman", Font.PLAIN, 28));

            String generatedCode = generateAlphanumericString(10);
            codeField.setText(generatedCode);

            Button back = new Button("BACK");
            back.setBounds(10, 20, 150, 40);
            back.setFont(new Font("Times New Roman", Font.ITALIC,18));
            
            back.addActionListener(e->{
           	 cardLayout.show(cardPanel, "ButtonPanel");
           });
            
            Button startButton = new Button("Start Session");
            startButton.setBounds(50, 180, 200, 50);
            startButton.setFont(new Font("Times New Roman", Font.BOLD, 22));

            startButton.addActionListener(e -> {
                storeSessionCodeInDB(generatedCode);
                cardLayout.show(cardPanel, "ChatPanel");
                try {
                    newClient = new Client(null);
                    newClient.startConnection("localhost", 8080);
                } catch (IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(panel, "Failed to connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
            panel.add(back);
            panel.add(label);
            panel.add(codeField);
            panel.add(startButton);
            return panel;
        }
        class create extends JPanel {  // Renamed to CustomPanel for clarity
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                // Colors for gradient
                Color color1 = new Color(Integer.parseInt("FF7E5F", 16)); // Peach color
                Color color2 = new Color(Integer.parseInt("FEB47B", 16)); // Light orange color

                // Create linear gradient
                LinearGradientPaint gradient = new LinearGradientPaint(
                        0, 0, getWidth(), getHeight(),  // Gradient from top-left to bottom-right
                        new float[]{0f, 1f},  // Start to end of the gradient
                        new Color[]{color1, color2}  // Colors used in the gradient
                );

                // Apply the gradient to the Graphics2D object and fill the entire panel
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        class decoration extends JPanel {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;

                Color color1 = new Color(Integer.parseInt("FF7E5F", 16));
                Color color2 = new Color(Integer.parseInt("FEB47B", 16));
                LinearGradientPaint gradient = new LinearGradientPaint(
                        0, 0, getWidth(), getHeight(),                // Start (x1, y1) and End (x2, y2)
                        new float[]{0f, 1f},                         // Color distribution
                        new Color[]{color1, color2}                  // Colors in the gradient
                );

                // Apply gradient to the Graphics2D object
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight()); // Fill the background with gradient
            }
        }
        private void storeSessionCodeInDB(String code) {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO code (id) VALUES (?)")) {
                stmt.setString(1, code);
                stmt.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        // The JPanel for displaying chat interface in the app
        public JPanel createChatPanel() {
            JPanel panel = new JPanel() {
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    Color color1 = new Color(Integer.parseInt("FF7E5F", 16)); // Peach color
                    Color color2 = new Color(Integer.parseInt("FEB47B", 16)); // Light orange color

                    LinearGradientPaint gradient = new LinearGradientPaint(
                        0, 0, getWidth(), getHeight(),
                        new float[]{0f, 1f},
                        new Color[]{color1, color2}
                    );

                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
            };

            panel.setLayout(null);

            JLabel userLabel = new JLabel("Logged in as: " + LoginFrame.loggedInUsername);
            userLabel.setFont(new Font("Arial", Font.ITALIC, 20));
            userLabel.setBounds(1300, 16, 400, 30);
            panel.add(userLabel);

            JTextPane chatPane = new JTextPane();
            chatPane.setEditable(false);
            chatPane.setFont(new Font("Arial", Font.PLAIN, 18));
            StyledDocument doc = chatPane.getStyledDocument();

            JScrollPane scrollPane = new JScrollPane(chatPane);
            scrollPane.setBounds(50, 60, 1400, 550);

            JTextField chatInput = new JTextField();
            chatInput.setBounds(50, 620, 1200, 50);
            String placeholderText = "Type your message here...";
            chatInput.setText(placeholderText);
            chatInput.setFont(new Font("Arial", Font.PLAIN, 22));
            chatInput.setForeground(Color.GRAY);

            chatInput.addFocusListener(new java.awt.event.FocusListener() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    if (chatInput.getText().equals(placeholderText)) {
                        chatInput.setText("");
                        chatInput.setForeground(Color.BLACK);
                    }
                }

                @Override
                public void focusLost(java.awt.event.FocusEvent e) {
                    if (chatInput.getText().isEmpty()) {
                        chatInput.setForeground(Color.GRAY);
                        chatInput.setText(placeholderText);
                    }
                }
            });

            Button sendButton = new Button("Send");
            sendButton.setBounds(1300, 630, 100, 40);
            sendButton.setFont(new Font("Times New Roman", Font.BOLD, 22));
            sendButton.addActionListener(e -> {
                String userMessage = chatInput.getText();
                if (!userMessage.trim().isEmpty()) {
                    String senderName = LoginFrame.loggedInUsername;
                    appendMessage(doc, senderName + ": " + userMessage, false);
                    newClient.sendMessage(senderName + ": " + userMessage);
                    chatInput.setText("");
                }
            });

            Button endSessionButton = new Button("End Session");
            endSessionButton.setBounds(700, 16, 200, 40);
            endSessionButton.setFont(new Font("Times New Roman", Font.BOLD, 22));
            endSessionButton.addActionListener(e -> {
                saveChatToFile();          // Save chat to a .txt file
                saveChatToDatabase();      // Save chat to the database
                endSession();              // Handle session termination
            });

            new Thread(() -> {
                try {
                    while (true) {
                        if (newClient != null) {
                            String serverMessage = newClient.receiveMessage();
                            if (serverMessage != null) {
                                String[] parts = serverMessage.split(": ", 2);
                                if (parts.length == 2) {
                                    String otherUserName = parts[0];
                                    String message = parts[1];
                                    SwingUtilities.invokeLater(() -> appendMessage(doc, otherUserName + ": " + message, true));
                                }
                            }
                        }
                        Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();

            panel.add(scrollPane);
            panel.add(chatInput);
            panel.add(sendButton);
            panel.add(endSessionButton);
            return panel;
        }

        private void endSession() {
            // Notify other user that session is terminated
            if (newClient != null) {
                newClient.sendMessage("SESSION_TERMINATED:" + LoginFrame.loggedInUsername);
            }
            
            // Save chat to file and database
            saveChatToFile();
            saveChatToDatabase();

            // Show dialog and exit the chat
            JOptionPane.showMessageDialog(null, "Chat has been saved. Ending session.");
            System.exit(0); // Close the application or return to the login page
        }

        private static StringBuilder chatContent = new StringBuilder(); // Store chat content

        private static void appendMessage(StyledDocument doc, String message, boolean isRightAligned) {
            try {
                Style style = doc.addStyle("MessageStyle", null);
                StyleConstants.setForeground(style, isRightAligned ? Color.RED : Color.BLACK);
                StyleConstants.setAlignment(style, isRightAligned ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);

                int len = doc.getLength();
                doc.insertString(len, message + "\n", style);
                chatContent.append(message).append("\n");
                doc.setParagraphAttributes(len, message.length(), style, false);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }

        // Method to save chat content to a text file
        private void saveChatToFile() {
            // Generate a unique filename using the username and timestamp
            String filename = "chat_" + LoginFrame.loggedInUsername + "_" + System.currentTimeMillis() + ".txt";

            try (FileWriter writer = new FileWriter(filename)) {
                // Write the entire chat content to the file
                writer.write(chatContent.toString());
                JOptionPane.showMessageDialog(null, "Chat saved to file: " + filename);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error saving chat to file!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        // Method to save chat content to a database
        private void saveChatToDatabase() {
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO history (username, chat_content, timestamp) VALUES (?, ?, ?)")) {
                stmt.setString(1, LoginFrame.loggedInUsername);
                stmt.setString(2, chatContent.toString());
                stmt.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis()));
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to save chat to database!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void endSessionOnServer() {
            if (newClient != null) {
                newClient.sendMessage("SESSION_TERMINATED:" + LoginFrame.loggedInUsername);
            }
        }
        private void redirectToHomeScreen() {
        	 new LoginFrame();
        	 SwingUtilities.invokeLater(() -> {
        	        // Use `this.dispose()` to close the current ChatPanel window.
        	        // `this` refers to the current instance of the ChatPanel class (or the window/frame)
        	        JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(this); // Get the parent frame
        	        if (currentFrame != null) {
        	            currentFrame.dispose(); // Dispose of the current chat panel (close it)
        	        }
        	    });
        }

        static class ChatServer {
            private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
            private static boolean sessionActive = true;  // Flag to indicate session state

            public static void startServer() {
                try (ServerSocket serverSocket = new ServerSocket(8080)) {
                    System.out.println("Server started on port 8080");
                    while (true) {
                        Socket socket = serverSocket.accept();
                        ClientHandler clientHandler = new ClientHandler(socket);
                        clientHandlers.add(clientHandler);
                        new Thread(clientHandler).start();
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            public static void terminateSession() {
                try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                     PreparedStatement stmt = conn.prepareStatement("UPDATE session_control SET is_terminated = TRUE WHERE id = 1")) {
                    stmt.executeUpdate();  // Update the session to "terminated"
                    broadcastSessionTermination();  // Notify all clients
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Broadcast session termination to all connected clients
            public static void broadcastSessionTermination() {
                for (ClientHandler clientHandler : clientHandlers) {
                }
            }

            // Broadcast messages to all connected clients
            public static void broadcastMessage(String message) {
                for (ClientHandler clientHandler : clientHandlers) {
                    clientHandler.sendMessage(message);  // Send message to each client
                }
            }

            // ClientHandler class that handles the communication with each client
            static class ClientHandler implements Runnable {
                private Socket socket;
                private PrintWriter out;
                private BufferedReader in;
                private String clientName;

                public ClientHandler(Socket socket) {
                    this.socket = socket;
                    try {
                        this.out = new PrintWriter(socket.getOutputStream(), true);
                        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        this.clientName = socket.getInetAddress().getHostName(); // Set clientName to the client's address
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void run() {
                    try {
                        String message;
                        while ((message = in.readLine()) != null) {
                            if ("END SESSION".equalsIgnoreCase(message)) {
                                sessionActive = false;
                                broadcastSessionTermination();  // Notify all clients that session is ending
                                break;
                            } else {
                                broadcastMessage(clientName + ": " + message);  // Broadcast the message from the client
                            }
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        clientHandlers.remove(this);  // Remove this client from the list when connection is closed
                    }
                }

                // Send message to the client
                public void sendMessage(String message) {
                    out.println(message);
                }
            }
        }

        // Client class to handle communication with the server
        static class Client {
            private MyApplet myAppletInstance;  // Reference to MyApplet instance
            private Socket socket;
            private PrintWriter out;
            private BufferedReader in;

            // Constructor for Client class
            public Client(MyApplet myAppletInstance) {
                this.myAppletInstance = myAppletInstance;  // Initialize the MyApplet instance
            }

            public void startConnection(String ip, int port) throws IOException {
                socket = new Socket(ip, port);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }

            public void sendMessage(String msg) {
                out.println(msg);  // Send a message to the server
            }

            public String receiveMessage() throws IOException {
                return in.readLine();  // Receive a message from the server
            }

            public void closeConnection() {
                try {
                    socket.close();  // Close the socket connection
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            public void checkSessionTermination() {
                new Thread(() -> {
                    try {
                        while (true) {
                            if (newClient != null) {
                                String serverMessage = newClient.receiveMessage();  // Receive message from server

                                if (serverMessage != null) {
                                    if (serverMessage.startsWith("SESSION_TERMINATED:")) {
                                        // Handle session termination logic
                                        SwingUtilities.invokeLater(() -> {
                                            // Get the current window from a valid component inside your JApplet
                                            JFrame currentFrame = (JFrame) SwingUtilities.getWindowAncestor(cardPanel);  // Use cardPanel or any other component
                                            if (currentFrame != null) {
                                                currentFrame.dispose();  // Close the current window
                                            }

                                            // Create a new window for displaying chat history
                                            JFrame newFrame = new JFrame("Chat History");
                                            newFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                            newFrame.setSize(800, 600);

                                            // Assuming createBorderPanel() returns your existing BorderPanel with history
                                            JPanel borderPanel = createBorderPanel(); 
                                            newFrame.add(borderPanel);  // Add BorderPanel which contains the chat history
                                            newFrame.setVisible(true);  // Display the new frame with the history
                                        });
                                    } else {
                                        // Parse and handle received message
                                        String[] parts = serverMessage.split(": ", 2);
                                        if (parts.length == 2) {
                                            String otherUserName = parts[0];
                                            String message = parts[1];
                                            StyledDocument doc = null;
                                            SwingUtilities.invokeLater(() -> appendMessage(doc, otherUserName + ": " + message, true));  // Display message in chat panel
                                        }
                                    }
                                }
                            }
                            Thread.sleep(100);  // Wait before checking again
                        }
                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }).start();
            }

            // Method to create and return BorderPanel
            private JPanel createBorderPanel() {
                // Access MyApplet via an instance of Chatlogix
                Chatlogix outer = new Chatlogix(); // Create instance of outer class Chatlogix
                return outer.new MyApplet().new BorderPanel();  // Create and return BorderPanel from MyApplet instance
            }


      
        }}}

