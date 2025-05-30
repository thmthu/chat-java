package org.example.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

public class ButtonCustom extends JButton {

    public ButtonCustom(String text) {
        super(text);
        setFont(new Font("Arial", Font.BOLD, 14));
        setFocusPainted(false);
        setBackground(Color.decode("#99CCFF"));
        setForeground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setContentAreaFilled(true);
        setOpaque(true);

        setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());
                super.paint(g, c);
                g2.dispose();
            }
        });
    }
}
