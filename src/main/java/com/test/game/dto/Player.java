package com.test.game.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.socket.WebSocketSession;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Player {
    private String nickname;
    private double amount;
    @JsonIgnore
    private WebSocketSession session;

}

