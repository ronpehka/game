package com.test.game.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
public class RoundResultResponse {
    private final String type = "roundResult";
    private final int winningNumber;
    private final List<Player> winners;
}
