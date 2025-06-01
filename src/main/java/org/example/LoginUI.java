package org.example;

import org.example.components.ButtonCustom;
import org.example.data.GlobalData;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LoginUI extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton signupButton;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public LoginUI() {
        setTitle("Chat App - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        
        // Main panel with white background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Logo or app name at top
        JLabel titleLabel = new JLabel("Chat Application", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.decode("#99CCFF"));
        titleLabel.setBorder(new EmptyBorder(0, 0, 20, 0));
        
        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        
        // Username field
        usernameField = new JTextField(20);
        styleTextField(usernameField);
        JPanel usernamePanel = createLabeledField("Username:", usernameField);
        
        // Password field
        passwordField = new JPasswordField(20);
        styleTextField(passwordField);
        JPanel passwordPanel = createLabeledField("Password:", passwordField);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        loginButton = new ButtonCustom("Login");
        signupButton = new JButton("Create Account");
        signupButton.setForeground(Color.decode("#99CCFF"));
        signupButton.setBorderPainted(false);
        signupButton.setContentAreaFilled(false);
        signupButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(signupButton);
        
        // Add components to form
        formPanel.add(usernamePanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(passwordPanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(buttonPanel);
        
        // Add components to main panel
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Add main panel to frame
        getContentPane().add(mainPanel);
        
        // Set up event handlers
        loginButton.addActionListener(e -> handleLogin());
        
        signupButton.addActionListener(e -> {
            dispose(); // Close login window
            new SignupUI().setVisible(true);
        });
    }
    
    private void styleTextField(JTextField textField) {
        Color borderColor = Color.decode("#99CCFF");
        textField.setBorder(new CompoundBorder(
                new LineBorder(borderColor, 1, true),
                new EmptyBorder(8, 10, 8, 10)
        ));
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
    }
    
    private JPanel createLabeledField(String labelText, Component field) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(Color.decode("#99CCFF"));
        panel.add(label, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
    
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please enter both username and password",
                "Login Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");
        
        executorService.submit(() -> {
            try {
                // Make API call to login
                String jsonPayload = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}",
                    username, password);
                
                URL url = new URL("http://localhost:8081/api/login");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    
                    // Parse JSON response
                    JsonObject jsonResponse = JsonParser.parseString(response.toString())
                                                      .getAsJsonObject();
                    
                    // Save user ID to global data
                    GlobalData.userId = jsonResponse.get("id").getAsString();
                    
                    // Switch to main chat UI
                    SwingUtilities.invokeLater(() -> {
                        dispose(); // Close login window
                        new ChatUI().setVisible(true);
                    });
                    
                } else {
                    // Show error message
                    SwingUtilities.invokeLater(() -> {
                        loginButton.setEnabled(true);
                        loginButton.setText("Login");
                        JOptionPane.showMessageDialog(this,
                            "Login failed. Please check your credentials.",
                            "Login Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    JOptionPane.showMessageDialog(this,
                        "Connection error: " + ex.getMessage(),
                        "Login Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new LoginUI().setVisible(true);
        });
    }
}