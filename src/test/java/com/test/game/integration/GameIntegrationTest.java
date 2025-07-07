package com.test.game.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GameIntegrationTest {

    @LocalServerPort
    private int port;

    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

    private WebSocketSession session;

    @BeforeEach
    public void setup() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        session = client
                .doHandshake(new TestWebSocketHandler(), null, URI.create("ws://localhost:" + port + "/ws/game"))
                .get();
        session.sendMessage(new TextMessage("NICKNAME:PlayerTest"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    @Test
    public void testBetAndReceiveResult() throws Exception {
        // Send a bet message
        session.sendMessage(new TextMessage("BET:{\"nickname\":\"PlayerTest\",\"number\":5,\"amount\":100}"));

        // Expect a result in the next 15 seconds
        String message = messages.poll(15, TimeUnit.SECONDS);

        assertNotNull(message, "Expected WIN, LOSE or WINNERS message but got none");

        // Validate the content
        assertTrue(message.startsWith("WIN") || message.startsWith("LOSE") || message.startsWith("WINNERS"),
                "Unexpected message: " + message);
    }

    private class TestWebSocketHandler extends AbstractWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messages.offer(message.getPayload());
        }
    }
}

