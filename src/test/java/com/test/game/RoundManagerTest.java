package com.test.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.game.dto.Bet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoundManagerTest {

    private RoundManager roundManager;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        roundManager = new RoundManager();
        session = mock(WebSocketSession.class);
    }

    @Test
    void addPlayer_playerAddedSuccessfully() {
        assertDoesNotThrow(() -> roundManager.addPlayer("Alice", session));
    }

    @Test
    void addBet_betAddedSuccessfully() {
        Bet bet = new Bet("Alice", 5, 100);
        assertDoesNotThrow(() -> roundManager.addBet(bet));
    }

    @Test
    void resolveRound_messageSentSuccessfully() throws Exception {
        when(session.isOpen()).thenReturn(true);

        roundManager.addPlayer("Bob", session);
        roundManager.addBet(new Bet("Bob", 7, 10.0));

        roundManager.resolveRound();

        verify(session, atLeastOnce()).sendMessage(any(TextMessage.class));
    }

    @Test
    void resolveRound_doesNotSendMessagesForUnknownPlayer() throws Exception {
        roundManager.addBet(new Bet("Ghost", 3, 5.0));
        roundManager.resolveRound();
        verify(session, never()).sendMessage(any());
    }

    @Test
    void resolveRound_doesNotSendMessagesWhenSessionClosed() throws Exception {
        when(session.isOpen()).thenReturn(false);
        roundManager.addPlayer("Carol", session);
        roundManager.addBet(new Bet("Carol", 2, 10.0));
        roundManager.resolveRound();
        verify(session, never()).sendMessage(any());
    }

    @Test
    void resolveRound_handlesIOExceptionDuringBroadcast() throws Exception {
        WebSocketSession badSession = mock(WebSocketSession.class);
        when(badSession.isOpen()).thenReturn(true);
        doThrow(new java.io.IOException("Simulated error")).when(badSession).sendMessage(any());

        roundManager.addPlayer("FailUser", badSession);
        roundManager.addBet(new Bet("FailUser", 1, 10.0));

        roundManager.resolveRound();

        verify(badSession, atLeastOnce()).sendMessage(any());
    }

    @Test
    void getPlayerSession_returnsSessionForExistingPlayer() {

        WebSocketSession session = mock(WebSocketSession.class);
        String nickname = "Player1";
        roundManager.addPlayer(nickname, session);
        WebSocketSession retrieved = roundManager.getPlayerSession(nickname);
        assertNotNull(retrieved);
        assertEquals(session, retrieved);
    }

    @Test
    void getPlayerSession_returnsNullForUnknownPlayer() {
        WebSocketSession retrieved = roundManager.getPlayerSession("Unknown");
        assertNull(retrieved);
    }

    @Test
    void resolveRound_whenBetMatchesWinningNumberTheSendWinMessage() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        ObjectMapper objectMapper = new ObjectMapper();
        Random mockRandom = mock(Random.class);
        when(mockRandom.nextInt(10)).thenReturn(4);

        RoundManager manager = new RoundManager(objectMapper, mockRandom);
        manager.addPlayer("Alice", session);
        manager.addBet(new Bet("Alice", 5, 100.0));

        manager.resolveRound();

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, times(2)).sendMessage(captor.capture());

        List<TextMessage> messages = captor.getAllValues();
        assertTrue(messages.get(0).getPayload().startsWith("WIN:"));
        assertTrue(messages.get(1).getPayload().startsWith("WINNERS:"));
    }

    @Test
    void resolveRound_sendErrorMessageWhenHandlingJsonProcessingException() throws Exception {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        doThrow(new JsonProcessingException("JSON error"){}).when(mockMapper).writeValueAsString(any());

        RoundManager roundManager = new RoundManager(mockMapper, new Random());

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);

        roundManager.addPlayer("player1", session);
        Bet bet = new Bet("player1", 5, 10);
        roundManager.addBet(bet);

        roundManager.resolveRound();

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());

        boolean foundErrorMessage = captor.getAllValues().stream()
                .anyMatch(msg -> msg.getPayload().startsWith("ERROR: General error: JSON error"));

        assertTrue(foundErrorMessage);
    }
}
