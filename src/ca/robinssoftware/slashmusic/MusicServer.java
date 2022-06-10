/*
Copyright 2022 Robins Software

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package ca.robinssoftware.slashmusic;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.java_websocket.WebSocket;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class MusicServer extends WebSocketServer {

    final InetSocketAddress address;

    HashSet<UUID> links = new HashSet<>();
    HashSet<ConnectionData> connections = new HashSet<>();

    MusicServer(InetSocketAddress address) {
        super(address);
        this.address = address;
    }

    public void sync(UUID player) {
        for (ConnectionData data : connections)
            if (data.player.equals(player))
                data.connection.send(MusicPlugin.instance.thread.getPacket());
    }

    public boolean isConnected(UUID player) {
        for (ConnectionData data : connections)
            if (data.player.equals(player))
                return true;

        return false;
    }

    public void broadcastToPlayers(String message) {
        for (ConnectionData data : connections) {
            Player player = Bukkit.getPlayer(data.player);

            if (player != null)
                player.sendMessage(message);
        }
    }

    @Override
    public void broadcast(String message) {
        try {
            for (ConnectionData data : connections)
                data.connection.send(message);
        } catch (WebsocketNotConnectedException e) {
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        ConnectionData target = null;

        for (ConnectionData data : connections)
            if (data.connection.equals(conn))
                target = data;

        Bukkit.getPlayer(target.player).sendMessage("§7>> §dYou have left the music channel. §7<<");

        connections.remove(target);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ConnectionData target = null;

        for (ConnectionData data : connections)
            if (data.connection.equals(conn))
                target = data;

        if (target == null) {
            // handshake
            try {
                UUID player = UUID.fromString(message);

                if (links.contains(player)) {
                    connections.add(new ConnectionData(conn, player));
                    links.remove(player);

                    Bukkit.getPlayer(player).sendMessage("§7>> §dYou have joined the music channel. §7<<");

                    conn.send("connected");
                    conn.send(MusicPlugin.instance.thread.getPacket());
                } else {
                    conn.send("invalid");
                }
            } catch (IllegalArgumentException e) {
                conn.send("invalid");
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
    }

    @Override
    public void onStart() {
    }

    private static class ConnectionData {

        WebSocket connection;
        UUID player;

        ConnectionData(WebSocket connection, UUID player) {
            this.connection = connection;
            this.player = player;
        }

    }

}
