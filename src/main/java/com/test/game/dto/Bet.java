package com.test.game.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class Bet {
    private String nickname;
    private int number;
    private double amount;


}
