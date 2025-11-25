package com.nbtstreaming;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class PositionStreamSocketServer extends WebSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(Nbtstreamv2.MOD_ID + "-ws");

    public PositionStreamSocketServer(int port) {
        super(new InetSocketAddress(port));
        setReuseAddr(true);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("Client connected from {}.", conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("Client {} disconnected (code={}, reason='{}', remote={}).",
                conn.getRemoteSocketAddress(), code, reason, remote);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        LOGGER.info("Ignoring incoming message from {}.", conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.error("WebSocket error for {}.", conn == null ? "server" : conn.getRemoteSocketAddress(), ex);
    }

    @Override
    public void onStart() {
        LOGGER.info("Position stream WebSocket server started on {}.", getAddress());
    }

    public void broadcastPayload(String payload) {
        broadcast(payload);
    }
}
