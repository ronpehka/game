package com.test.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.game.dto.Bet;
import com.test.game.exception.BetException;
import com.test.game.util.BetValidator;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static com.test.game.util.ErrorUtil.sendError;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final RoundManager roundManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public GameWebSocketHandler(RoundManager roundManager) {
        this.roundManager = roundManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {}

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        try {
            if (payload.startsWith("NICKNAME:")) {
                String nickname = payload.split(":", 2)[1];
                roundManager.addPlayer(nickname, session);
            } else if (payload.startsWith("BET:")) {
                Bet bet = objectMapper.readValue(payload.substring(4), Bet.class);
                if (roundManager.getPlayerSession(bet.getNickname()) != null) {
                    boolean isValid = BetValidator.isValid(bet);
                    if (isValid) {
                        roundManager.addBet(bet);
                    } else {
                        throw new BetException("Invalid bet");
                    }
                } else {
                    sendError(session, "Register player first, then place bet");
                }
            } else{
                sendError(session, "Valid commands: NICKNAME:Player1 and BET:{\"nickname\":\"Player1\",\"number\":1,\"amount\":100}");
            }
        } catch (BetException be) {
            sendError(session, "Error placing bet: " + be.getMessage());
        } catch (Exception e) {
            sendError(session, "General error: " + e.getMessage());
        }
    }
}
