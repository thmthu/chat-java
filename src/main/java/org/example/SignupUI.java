package org.example;

import org.example.components.ButtonCustom;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignupUI extends JFrame {
    private JTextField usernameField;
    // Removed email field
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private JButton signupButton;
    private JButton backToLoginButton;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SignupUI() {
        setTitle("Chat App - Sign Up");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 450); // Reduced height since we removed email field
        setLocationRelativeTo(null);
        
        // Main panel with white background
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Title at top
        JLabel titleLabel = new JLabel("Create Account", JLabel.CENTER);
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
        
        // Removed email field section
        
        // Password field
        passwordField = new JPasswordField(20);
        styleTextField(passwordField);
        JPanel passwordPanel = createLabeledField("Password:", passwordField);
        
        // Confirm password field
        confirmPasswordField = new JPasswordField(20);
        styleTextField(confirmPasswordField);
        JPanel confirmPanel = createLabeledField("Confirm Password:", confirmPasswordField);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        signupButton = new ButtonCustom("Sign Up");
        backToLoginButton = new JButton("Back to Login");
        backToLoginButton.setForeground(Color.decode("#99CCFF"));
        backToLoginButton.setBorderPainted(false);
        backToLoginButton.setContentAreaFilled(false);
        backToLoginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(signupButton);
        buttonPanel.add(backToLoginButton);
        
        // Add components to form
        formPanel.add(usernamePanel);
        formPanel.add(Box.createVerticalStrut(10));
        // Removed email panel addition
        formPanel.add(passwordPanel);
        formPanel.add(Box.createVerticalStrut(10));
        formPanel.add(confirmPanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(buttonPanel);
        
        // Add components to main panel
        mainPanel.add(titleLabel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        
        // Add main panel to frame
        getContentPane().add(mainPanel);
        
        // Set up event handlers
        signupButton.addActionListener(e -> handleSignup());
        
        backToLoginButton.addActionListener(e -> {
            dispose(); // Close signup window
            new LoginUI().setVisible(true);
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
    
    private void handleSignup() {
        String username = usernameField.getText().trim();
        // Removed email variable
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        
        // Basic validation - removed email check
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all fields",
                "Signup Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, 
                "Passwords don't match",
                "Signup Error", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        signupButton.setEnabled(false);
        signupButton.setText("Creating account...");
        
        executorService.submit(() -> {
            try {
                // Make API call to signup - removed email from payload
                String jsonPayload = String.format(
                    "{\"username\":\"%s\",\"password\":\"%s\"}",
                    username, password);
                
                URL url = new URL("http://localhost:8081/api/signup");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                
                if (responseCode == HttpURLConnection.HTTP_OK || 
                    responseCode == HttpURLConnection.HTTP_CREATED) {
                    
                    // Show success message and return to login
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this,
                            "Account created successfully! Please login.",
                            "Signup Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                            
                        dispose();
                        new LoginUI().setVisible(true);
                    });
                } else {
                    // Show error message
                    SwingUtilities.invokeLater(() -> {
                        signupButton.setEnabled(true);
                        signupButton.setText("Sign Up");
                        JOptionPane.showMessageDialog(this,
                            "Failed to create account. Please try again.",
                            "Signup Error",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    signupButton.setEnabled(true);
                    signupButton.setText("Sign Up");
                    JOptionPane.showMessageDialog(this,
                        "Connection error: " + ex.getMessage(),
                        "Signup Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }
}