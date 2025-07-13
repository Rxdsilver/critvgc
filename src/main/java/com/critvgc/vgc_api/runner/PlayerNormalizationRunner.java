package com.critvgc.vgc_api.runner;

import com.critvgc.vgc_api.service.PlayerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PlayerNormalizationRunner implements CommandLineRunner {

    private final PlayerService playerService;

    public PlayerNormalizationRunner(PlayerService playerService) {
        this.playerService = playerService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Starting player name normalization...");
        playerService.uniformizeFullnamesInDatabase();
        System.out.println("Player name normalization done.");
    }
}
