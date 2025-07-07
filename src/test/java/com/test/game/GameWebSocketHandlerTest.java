package com.test.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.game.dto.Bet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameWebSocketHandlerTest {

    private RoundManager roundManager;
    private WebSocketSession session;
    private GameWebSocketHandler handler;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        roundManager = mock(RoundManager.class);
        session = mock(WebSocketSession.class);
        handler = new GameWebSocketHandler(roundManager);
        objectMapper = new ObjectMapper();
    }

    @Test
    void handleTextMessage_playerAddedSuccessfully(){
        TextMessage message = new TextMessage("NICKNAME:JohnDoe");
        handler.handleTextMessage(session, message);
        verify(roundManager).addPlayer(eq("JohnDoe"), eq(session));
    }

    @Test
    void handleTextMessage_betAddedSuccessfully() throws Exception {
        Bet bet = new Bet("JohnDoe", 5, 100.0);
        String betJson = objectMapper.writeValueAsString(bet);
        TextMessage message = new TextMessage("BET:" + betJson);

        when(roundManager.getPlayerSession("JohnDoe")).thenReturn(session);
        handler.handleTextMessage(session, message);
        verify(roundManager).addBet(any(Bet.class));
    }

    @Test
    void handleTextMessage_handleBetExceptionWhenAmountNegative() throws Exception {
        Bet invalidBet = new Bet("JohnDoe", 5, -10.0);
        String betJson = objectMapper.writeValueAsString(invalidBet);
        TextMessage message = new TextMessage("BET:" + betJson);

        when(roundManager.getPlayerSession("JohnDoe")).thenReturn(session);

        handler.handleTextMessage(session, message);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String errorMessage = captor.getValue().getPayload();
        assertTrue(errorMessage.startsWith("ERROR:"));
        assertTrue(errorMessage.contains("Invalid bet"));
    }

    @Test
    void handleTextMessage_handleExceptionWithMalformedMessage() throws Exception {
        TextMessage message = new TextMessage("BADMESSAGE");

        handler.handleTextMessage(session, message);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String errorMessage = captor.getValue().getPayload();
        assertTrue(errorMessage.startsWith("ERROR:"));
        assertTrue(errorMessage.contains("Valid commands: "));
    }

    @Test
    void handleTextMessage_sendRegisterFirstErrorWhenPlayerNotRegistered() throws Exception {
        Bet bet = new Bet("UnregisteredUser", 3, 50.0);
        String betJson = objectMapper.writeValueAsString(bet);
        TextMessage message = new TextMessage("BET:" + betJson);


        when(roundManager.getPlayerSession("UnregisteredUser")).thenReturn(null);

        handler.handleTextMessage(session, message);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String errorMessage = captor.getValue().getPayload();
        assertTrue(errorMessage.contains("Register player first"));
    }

    @Test
    void handleTextMessage_sendGeneralErrorWhenInvalidJson() throws Exception {

        String invalidJson = "BET:{\"nickname\":\"John\",\"number\":3,\"amount\":100";
        TextMessage message = new TextMessage(invalidJson);

        handler.handleTextMessage(session, message);

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());

        String errorMessage = captor.getValue().getPayload();
        assertTrue(errorMessage.startsWith("ERROR: General error:"));
    }

}
