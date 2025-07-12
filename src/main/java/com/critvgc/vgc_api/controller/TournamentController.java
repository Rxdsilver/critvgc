package com.critvgc.vgc_api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.critvgc.vgc_api.model.Tournament;
import com.critvgc.vgc_api.repository.TournamentRepository;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@CrossOrigin
public class TournamentController {

    private final TournamentRepository tournamentRepository;

    public TournamentController(TournamentRepository tournamentRepository) {
        this.tournamentRepository = tournamentRepository;
    }

    // GET /api/tournaments
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        List<Tournament> tournaments = tournamentRepository.findAll();
        return ResponseEntity.ok(tournaments); // 200 OK
    }

    // POST /api/tournaments
    @PostMapping
    public ResponseEntity<Tournament> createTournament(@RequestBody Tournament tournament) {
        Tournament saved = tournamentRepository.save(tournament);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved); // 201 Created
    }
}
