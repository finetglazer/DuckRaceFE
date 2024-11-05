package client.race;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class RaceWebSocketClient extends WebSocketClient {

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
        // Handle incoming messages (e.g., timer updates, race updates)
        raceRoomPanel.handleServerMessage(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        System.out.println("Disconnected from race server");
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }
}