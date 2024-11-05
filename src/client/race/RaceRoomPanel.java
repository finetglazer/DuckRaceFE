package client.race;

import client.ClosablePanel;
import client.DuckRaceClient;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RaceRoomPanel extends JPanel implements ClosablePanel {
    private DuckRaceClient mainFrame;
    private String playerId;
    private WebSocketClient webSocketClient;
    private JLabel pointsLabel;
    private int playerPoints = 1000; // Initialize with default value
    private JLabel timerLabel;
    private JButton placeBetButton;
    private JTextField candidateField;
    private JTextField amountField;

    // For candidate movement
    private JPanel raceTrackPanel;
    private Map<Integer, JLabel> candidateLabels = new HashMap<>();
    private Map<Integer, Integer> candidateFinishTimes = new HashMap<>();
    private Map<Integer, Timer> candidateTimers = new HashMap<>();

    public RaceRoomPanel(DuckRaceClient mainFrame, String playerId) {
        this.mainFrame = mainFrame;
        this.playerId = playerId;
        initializeComponents();
        layoutComponents();
    }

    private void initializeComponents() {
        pointsLabel = new JLabel("Points: " + playerPoints);
        timerLabel = new JLabel("Betting Time Left: ");

        placeBetButton = new JButton("Place Bet");
        candidateField = new JTextField(5);
        amountField = new JTextField(5);

        placeBetButton.addActionListener(e -> {
            try {
                int candidateId = Integer.parseInt(candidateField.getText().trim());
                int amount = Integer.parseInt(amountField.getText().trim());

                Map<String, Object> betMessage = new HashMap<>();
                betMessage.put("type", "placeBet");
                betMessage.put("candidateId", candidateId);
                betMessage.put("amount", amount);

                String message = new ObjectMapper().writeValueAsString(betMessage);
                webSocketClient.send(message);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Invalid input.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Initialize race track panel
        raceTrackPanel = new JPanel();
        raceTrackPanel.setLayout(null); // Use absolute positioning
        raceTrackPanel.setPreferredSize(new Dimension(800, 300));
        raceTrackPanel.setBackground(Color.WHITE);

        // Initialize candidate labels
        for (int i = 1; i <= 5; i++) {
            JLabel candidateLabel = new JLabel("C" + i);
            candidateLabel.setOpaque(true);
            candidateLabel.setBackground(Color.BLUE);
            candidateLabel.setForeground(Color.WHITE);
            candidateLabel.setHorizontalAlignment(SwingConstants.CENTER);
            candidateLabel.setBounds(0, (i - 1) * 50, 50, 30); // Starting position
            candidateLabels.put(i, candidateLabel);
            raceTrackPanel.add(candidateLabel);
        }

        // Initialize WebSocket client
        try {
            // Encode the playerId to handle any special characters
            String encodedPlayerId = URLEncoder.encode(playerId, "UTF-8");
            String serverUriStr = "ws://localhost:8080/duckRace/race?playerId=" + encodedPlayerId;
            URI serverUri = new URI(serverUriStr);
            webSocketClient = new RaceWebSocketClient(serverUri, this);
            webSocketClient.connect();
        } catch (URISyntaxException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(pointsLabel);
        topPanel.add(timerLabel);

        JPanel betPanel = new JPanel();
        betPanel.add(new JLabel("Candidate ID:"));
        betPanel.add(candidateField);
        betPanel.add(new JLabel("Amount:"));
        betPanel.add(amountField);
        betPanel.add(placeBetButton);

        add(topPanel, BorderLayout.NORTH);
        add(raceTrackPanel, BorderLayout.CENTER);
        add(betPanel, BorderLayout.SOUTH);
    }

    public void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode rootNode = mapper.readTree(message);
                String type = rootNode.path("type").asText();
                switch (type) {
                    case "echo":
                        String nestedJsonString = rootNode.path("message").asText();
                        Map<String, Object> nestedMessageMap = mapper.readValue(nestedJsonString, Map.class);
                        System.out.println("Echoed message: " + nestedMessageMap);
                        break;
                    case "error":
                        String errorMsg = rootNode.path("message").asText();
                        JOptionPane.showMessageDialog(this, "Error from server: " + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                        break;
                    case "connectedToRaceRoom":
                        System.out.println("Connected to race room.");
                        break;
                    case "points":
                        int points = rootNode.path("points").asInt();
                        this.playerPoints = points;
                        pointsLabel.setText("Points: " + points);
                        break;
                    case "timer":
                        int remainingTime = rootNode.path("remainingTime").asInt();
                        timerLabel.setText("Betting Time Left: " + remainingTime + " seconds");
                        break;
                    case "raceFinishTimes":
                        JsonNode finishTimesNode = rootNode.path("finishTimes");
                        candidateFinishTimes.clear();
                        Iterator<String> fieldNames = finishTimesNode.fieldNames();
                        while (fieldNames.hasNext()) {
                            String key = fieldNames.next();
                            int candidateId = Integer.parseInt(key);
                            int finishTime = finishTimesNode.path(key).asInt();
                            candidateFinishTimes.put(candidateId, finishTime);
                        }
                        // Start the race animation
                        startRaceAnimation();
                        break;
                    case "raceStarted":
                        timerLabel.setText("Race in progress...");
                        break;
                    case "raceFinished":
                        int winnerId = rootNode.path("winner").asInt();
                        JOptionPane.showMessageDialog(this, "Race finished! Winner: Candidate " + winnerId);
                        // Reset candidate positions and timers
                        resetCandidatePositions();
                        // Re-enable betting controls
                        placeBetButton.setEnabled(true);
                        candidateField.setEnabled(true);
                        amountField.setEnabled(true);
                        break;
                    case "reward":
                        int reward = rootNode.path("reward").asInt();
                        JOptionPane.showMessageDialog(this, "You won " + reward + " points!");
                        break;
                    // Handle other message types...
                    default:
                        System.out.println("Received unknown message type: " + type);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void startRaceAnimation() {
        // Disable the betting controls
        placeBetButton.setEnabled(false);
        candidateField.setEnabled(false);
        amountField.setEnabled(false);

        // Record the start time
        long raceStartTime = System.currentTimeMillis();

        // Stop any existing timers and clear the map
        if (candidateTimers != null) {
            for (Timer timer : candidateTimers.values()) {
                if (timer.isRunning()) {
                    timer.stop();
                }
            }
            candidateTimers.clear();
        }

        // For each candidate, create a Timer to animate movement
        for (int candidateId : candidateFinishTimes.keySet()) {
            int finishTimeInSeconds = candidateFinishTimes.get(candidateId);
            JLabel candidateLabel = candidateLabels.get(candidateId);

            // Total distance to cover
            int trackLength = raceTrackPanel.getWidth() - 50; // Subtract candidate label width

            // Create a timer for the candidate
            Timer timer = new Timer(50, null);
            timer.addActionListener(e -> {
                long currentTime = System.currentTimeMillis();
                double elapsedTime = (currentTime - raceStartTime) / 1000.0; // in seconds

                double progress = elapsedTime / finishTimeInSeconds;
                if (progress >= 1.0) {
                    progress = 1.0;
                    ((Timer) e.getSource()).stop();
                }

                int newX = (int) (progress * trackLength);
                candidateLabel.setLocation(newX, candidateLabel.getY());
                raceTrackPanel.repaint();
            });
            timer.start();

            // Keep track of the timer
            candidateTimers.put(candidateId, timer);
        }
    }

    private void resetCandidatePositions() {
        // Stop all candidate timers
        if (candidateTimers != null) {
            for (Timer timer : candidateTimers.values()) {
                if (timer.isRunning()) {
                    timer.stop();
                }
            }
            candidateTimers.clear();
        }
        // Reset candidate positions
        for (int candidateId : candidateLabels.keySet()) {
            JLabel candidateLabel = candidateLabels.get(candidateId);
            candidateLabel.setLocation(0, (candidateId - 1) * 50);
        }
    }

    @Override
    public void closeConnection() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        }
    }

    // Inner class for WebSocket client
    private class RaceWebSocketClient extends WebSocketClient {
        private RaceRoomPanel raceRoomPanel;

        public RaceWebSocketClient(URI serverUri, RaceRoomPanel raceRoomPanel) {
            super(serverUri);
            this.raceRoomPanel = raceRoomPanel;
        }

        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Connected to race server");
        }

        @Override
        public void onMessage(String message) {
            System.out.println("Received: " + message);
            raceRoomPanel.handleServerMessage(message);
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Disconnected from race server. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);
        }

        @Override
        public void onError(Exception ex) {
            System.out.println("WebSocket error:");
            ex.printStackTrace();
        }
    }
}
