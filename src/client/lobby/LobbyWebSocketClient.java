package client.lobby;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class LobbyWebSocketClient extends WebSocketClient {

    private LobbyPanel lobbyPanel;

    public LobbyWebSocketClient(URI serverUri, LobbyPanel lobbyPanel) {
        super(serverUri);
        this.lobbyPanel = lobbyPanel;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        System.out.println("Connected to lobby server");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("Received: " + message);
        if (message.startsWith("PLAYER_ID:")) {
            // Extract and store the player ID
            String playerId = message.substring("PLAYER_ID:".length());
            lobbyPanel.setPlayerId(playerId);
            System.out.println("Player ID set to: " + playerId);
        } else {
            // Assume it's the player list in JSON format
            lobbyPanel.updatePlayerList(message);
        }
    }


    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from lobby server. Code: " + code + ", Reason: " + reason + ", Remote: " + remote);
    }

    @Override
    public void onError(Exception ex) {
        System.out.println("WebSocket error:");
        ex.printStackTrace();
    }
}
