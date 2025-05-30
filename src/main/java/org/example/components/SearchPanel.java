package org.example.components;

import javax.swing.*;
import java.awt.*;

public class SearchPanel extends JPanel {
        public SearchPanel() {
            JTextField searchField = new JTextField("Search...");
            searchField.setPreferredSize(new Dimension(200, 40));
            searchField.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            JButton searchBtn = new JButton("Search");
            searchBtn.setBorderPainted(false);
            searchBtn.setBackground(Color.decode("#99CCFF"));
            searchBtn.setForeground(Color.WHITE);
            searchBtn.setPreferredSize(new Dimension(100, 40));

            JPanel searchPanel = new JPanel(new BorderLayout());
            searchPanel.add(searchField, BorderLayout.CENTER);
            searchPanel.add(searchBtn, BorderLayout.EAST);
        }
}
