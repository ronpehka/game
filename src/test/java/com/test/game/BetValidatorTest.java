package com.test.game;


import com.test.game.dto.Bet;
import com.test.game.util.BetValidator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BetValidatorTest {

    @Test
    void isValid_betIsValid() {
        Bet validBet = new Bet("Player1", 5, 100);
        assertTrue(BetValidator.isValid(validBet));
    }

    @Test
    void isValid_returnFalseWhenInvalidNumber() {
        Bet bet = new Bet("Player1", 11, 100);
        assertFalse(BetValidator.isValid(bet));
    }

    @Test
    void isValid_returnFalseWhenInvalidAmount() {
        Bet bet = new Bet("Player1", 5, 0);
        assertFalse(BetValidator.isValid(bet));
    }

    @Test
    void isValid_returnFalseWhenNullNickname() {
        Bet bet = new Bet(null, 5, 100);
        assertFalse(BetValidator.isValid(bet));
    }

    @Test
    void isValid_returnFalseWhenEmptyNickname() {
        Bet bet = new Bet("", 5, 100);
        assertFalse(BetValidator.isValid(bet));
    }
}
