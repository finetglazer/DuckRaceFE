package client;

import client.lobby.LobbyPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DuckRaceClient extends JFrame {
    public DuckRaceClient() {
        setTitle("DuckRace Betting Game");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize and add the lobby panel
        LobbyPanel lobbyPanel = new LobbyPanel(this);
        add(lobbyPanel);

        // Set the window to be visible
        setLocationRelativeTo(null); // Center the window
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                Component contentPane = getContentPane();
                if (contentPane instanceof ClosablePanel) {
                    ((ClosablePanel) contentPane).closeConnection();
                }
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DuckRaceClient());
    }
}