package com.critvgc.vgc_api.controller;

import com.critvgc.vgc_api.model.Tournament;
import com.critvgc.vgc_api.repository.TournamentRepository;
import com.critvgc.vgc_api.service.TournamentDataLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@CrossOrigin
public class TournamentController {

    private final TournamentRepository tournamentRepository;
    private final TournamentDataLoader tournamentDataLoader;

    public TournamentController(TournamentRepository tournamentRepository,TournamentDataLoader tournamentDataLoader) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentDataLoader = tournamentDataLoader;
    }

    // GET /api/tournaments
    @GetMapping
    public ResponseEntity<List<Tournament>> getAllTournaments() {
        List<Tournament> tournaments = tournamentRepository.findAll();
        return ResponseEntity.ok(tournaments);
    }

    // POST /api/tournaments/import?code=...
    @PostMapping("/import")
    public ResponseEntity<String> importOrUpdateTournament(@RequestParam String code) {
        boolean success = tournamentDataLoader.importOrUpdateTournament(code);
        if (success) {
            return ResponseEntity.ok("Tournament successfully imported or updated.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred during import.");
        }
    }

}
