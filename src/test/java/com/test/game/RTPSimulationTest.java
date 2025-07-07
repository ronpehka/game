package com.test.game;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class RTPSimulationTest {
    private static final Logger log = LoggerFactory.getLogger(RTPSimulationTest.class);

    @Test
    void simulateMillionRounds() throws InterruptedException, ExecutionException {
        int rounds = 1_000_000;
        double totalWon = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(24)) {
            List<Callable<Double>> tasks = IntStream.range(0, rounds)
                    .mapToObj(i -> (Callable<Double>) () -> {
                        int betNumber = new Random().nextInt(10) + 1;
                        int winNumber = new Random().nextInt(10) + 1;
                        return betNumber == winNumber ? 9.9 : 0;
                    }).collect(Collectors.toList());

            try {
                List<Future<Double>> results = executor.invokeAll(tasks);
                for (Future<Double> future : results) {
                    totalWon += future.get();
                }
                double rtp = (totalWon / (double) rounds) * 100;
                log.info("Spent: {}, Won: {}, RTP: {}%",
                        String.format("%.2f", (double) rounds),
                        String.format("%.2f", totalWon),
                        String.format("%.2f", rtp));
            } finally {
                executor.shutdown();
            }
        }
    }

}
