package com.test.game.util;

import com.test.game.dto.Bet;

public class BetValidator {
    public static boolean isValid(Bet bet) {
        return bet != null &&
                bet.getNickname() != null &&
                !bet.getNickname().trim().isEmpty() &&
                bet.getNumber() >= 1 &&
                bet.getNumber() <= 10 &&
                bet.getAmount() > 0;
    }
}
