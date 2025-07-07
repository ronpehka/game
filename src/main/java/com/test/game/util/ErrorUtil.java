package com.test.game.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

public class ErrorUtil {
    private static final Logger log = LoggerFactory.getLogger(ErrorUtil.class);
    public static void sendError(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage("ERROR: " + message));
        } catch (IOException ioEx) {
            try {
                if (session.isOpen()) {
                    session.close();
                }
            } catch (IOException closeEx) {
                log.error("Failed to close session: ", closeEx);
            }
        }
    }
}
