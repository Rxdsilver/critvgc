package com.critvgc.vgc_api.runner;

import com.critvgc.vgc_api.model.Player;
import com.critvgc.vgc_api.model.Match;
import com.critvgc.vgc_api.model.Team;
import com.critvgc.vgc_api.repository.PlayerRepository;
import com.critvgc.vgc_api.repository.MatchRepository;
import com.critvgc.vgc_api.repository.TeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PlayerDeduplicationRunner implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public PlayerDeduplicationRunner(PlayerRepository playerRepository,
                                     MatchRepository matchRepository,
                                     TeamRepository teamRepository) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.teamRepository = teamRepository;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting player deduplication...");

        List<Player> players = playerRepository.findAll();

        // Group by normalized fullname + country
        Map<String, List<Player>> grouped = players.stream()
                .collect(Collectors.groupingBy(p -> normalize(p.getFullname()) + "|" + p.getCountry()));

        int mergedCount = 0;

        for (List<Player> duplicates : grouped.values()) {
            if (duplicates.size() <= 1) continue;

            Player keeper = duplicates.get(0); // keep the first
            List<Player> toDelete = duplicates.subList(1, duplicates.size());

            for (Player duplicate : toDelete) {
                replaceReferences(duplicate, keeper);
                playerRepository.deleteById(duplicate.getId());
                mergedCount++;
            }
        }

        System.out.println("Player deduplication complete. Merged " + mergedCount + " duplicate(s).");
    }

    private void replaceReferences(Player from, Player to) {
        // Update matches
        List<Match> matches = matchRepository.findByPlayer1IdOrPlayer2Id(from.getId(), from.getId());
        for (Match match : matches) {
            boolean updated = false;

            if (from.getId().equals(match.getPlayer1Id())) {
                match.setPlayer1Id(to.getId());
                match.setPlayer1Name(to.getFullname());
                updated = true;
            }

            if (from.getId().equals(match.getPlayer2Id())) {
                match.setPlayer2Id(to.getId());
                match.setPlayer2Name(to.getFullname());
                updated = true;
            }

            if (updated) matchRepository.save(match);
        }

        // Update teams
        List<Team> teams = teamRepository.findByPlayerId(from.getId());
        for (Team team : teams) {
            team.setPlayerId(to.getId());
            team.setPlayerName(to.getFullname());
            teamRepository.save(team);
        }
    }

    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .toLowerCase();
    }
}
