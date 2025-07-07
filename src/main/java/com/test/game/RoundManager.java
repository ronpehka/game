package com.test.game;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.test.game.dto.Bet;
import com.test.game.dto.Player;
import com.test.game.dto.RoundResultResponse;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.test.game.util.ErrorUtil.sendError;

@Service
public class RoundManager {

    private static final Logger log = LoggerFactory.getLogger(RoundManager.class);
    private final List<Bet> currentBets = new ArrayList<>();
    private final Map<String, WebSocketSession> players = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final ObjectMapper objectMapper;
    private final Random random;

    public RoundManager() {
        this(new ObjectMapper(), new Random());
    }

    public RoundManager(ObjectMapper objectMapper, @Nullable Random random) {
        this.objectMapper = objectMapper;
        this.random = (random != null) ? random : new Random();
    }

    @PostConstruct
    public void startGameLoop() {
        scheduler.scheduleAtFixedRate(this::resolveRound, 10, 10, TimeUnit.SECONDS);
    }

    public void addBet(Bet bet) {
        currentBets.add(bet);
    }

    public void addPlayer(String nickname, WebSocketSession session) {
        players.put(nickname, session);
    }

    public WebSocketSession getPlayerSession(String nickname) {
      return players.get(nickname);
    }

   public void resolveRound() {
        WebSocketSession session = null;
       int winningNumber = random.nextInt(10) + 1;
        List<Player> winners = new ArrayList<>();

        for (Bet bet : currentBets) {
            try {
                session = getPlayerSession(bet.getNickname());
                if (session != null && session.isOpen()) {
                    if (bet.getNumber() == winningNumber) {
                        double winnings = bet.getAmount() * 9.9;
                        winners.add(new Player(bet.getNickname(), winnings, session));
                        session.sendMessage(new TextMessage("WIN: " + winnings));
                    } else {
                        session.sendMessage(new TextMessage("LOSE"));
                    }
                }
            } catch (Exception e) {
                log.error("Error resolving round: ", e);
                if (session != null) {
                    sendError(session, e.getMessage());
                }
            }
        }

        try {
            RoundResultResponse response = new RoundResultResponse(winningNumber, winners);
            String summary = "WINNERS: " + objectMapper.writeValueAsString(response);
            broadcast(summary);
        } catch (JsonProcessingException jpe) {
            log.error("Failed to process JSON: ", jpe);
            if (session != null) {
                sendError(session, "General error: " + jpe.getMessage());
            }
        }
        currentBets.clear();
    }

    private void broadcast(String message) {
        players.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("Failed to send message: ", e);
                }
            }
        });
    }
}

