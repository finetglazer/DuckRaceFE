package client.lobby;

import client.ClosablePanel;
import client.DuckRaceClient;
import client.race.RaceRoomPanel;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class LobbyPanel extends JPanel implements ClosablePanel {
    private DuckRaceClient mainFrame;
    private JTextField nameField;
    private JButton enterRaceButton;
    private DefaultListModel<String> playerListModel;
    private JList<String> playerList;
    private String playerId;
    private LobbyWebSocketClient webSocketClient;

    @Override
    public void closeConnection() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
        System.out.println("Assigned Player ID: " + playerId);
        SwingUtilities.invokeLater(() -> enterRaceButton.setEnabled(true));
    }



    public void updatePlayerList(String playerListJson) {
        SwingUtilities.invokeLater(() -> {
            playerListModel.clear();
            ObjectMapper mapper = new ObjectMapper();
            try {
                Map<String, Object> messageMap = mapper.readValue(playerListJson, Map.class);
                String type = (String) messageMap.get("type");
                if ("playerUpdate".equals(type)) {
                    List<Map<String, Object>> players = (List<Map<String, Object>>) messageMap.get("players");
                    for (Map<String, Object> player : players) {
                        String name = (String) player.get("name");
                        playerListModel.addElement(name);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }





    public LobbyPanel(DuckRaceClient mainFrame) {
        this.mainFrame = mainFrame;
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        nameField = new JTextField(20);
        enterRaceButton = new JButton("Enter Race Room");
        enterRaceButton.setEnabled(false);
        playerListModel = new DefaultListModel<>();
        playerList = new JList<>(playerListModel);


        enterRaceButton.addActionListener(e -> {
            try {
                System.out.println("Enter Race Room button clicked.");
                String playerName = nameField.getText().trim();
                System.out.println("Player name entered: " + playerName);
                if (!playerName.isEmpty()) {
                    // Send SET_NAME message to the server
                    webSocketClient.send("SET_NAME:" + playerName);
                    System.out.println("Sent SET_NAME message to server.");

                    // Proceed to the race room
                    System.out.println("Player ID before entering race room: " + playerId);
                    if (playerId != null && !playerId.isEmpty()) {
                        RaceRoomPanel raceRoomPanel = new RaceRoomPanel(mainFrame, playerId);
                        System.out.println("Created RaceRoomPanel.");
                        mainFrame.setContentPane(raceRoomPanel);
                        mainFrame.revalidate();
                        mainFrame.repaint();
                        System.out.println("Switched to RaceRoomPanel.");

                        // Close the lobby WebSocket connection
                        webSocketClient.close();
                    } else {
                        JOptionPane.showMessageDialog(this, "Player ID not received yet. Please wait a moment.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Please enter your name.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "An error occurred: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

        });

        try {
            URI serverUri = new URI("ws://localhost:8080/duckRace");
            webSocketClient = new LobbyWebSocketClient(serverUri, this);
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Enter your name:"));
        topPanel.add(nameField);
        topPanel.add(enterRaceButton);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(playerList), BorderLayout.CENTER);
    }


    // Methods to update the player list will be added later
}
