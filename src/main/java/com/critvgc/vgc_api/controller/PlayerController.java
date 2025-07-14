package com.critvgc.vgc_api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.critvgc.vgc_api.model.Player;
import com.critvgc.vgc_api.repository.PlayerRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/players")
@CrossOrigin
public class PlayerController {

    PlayerRepository playerRepository;

    public PlayerController(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    // GET /api/players
    @GetMapping
    public ResponseEntity<List<Player>> searchPlayerByFullname(@RequestParam String fullname) {
        List<Player> players = playerRepository.findByFullname(fullname);
        if (players.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(players);
    }

    
    
    
}
