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

    System.out.println("Merged " + mergedCount + " duplicate player(s). Now checking for duplicate matches...");

    int removed = removeDuplicateMatches();
    System.out.println("Removed " + removed + " duplicate match(es).");

    System.out.println("Player deduplication complete.");
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

    private int removeDuplicateMatches() {
        List<Match> allMatches = matchRepository.findAll();

        Set<String> uniqueMatchKeys = new HashSet<>();
        List<Match> toDelete = new ArrayList<>();

        for (Match match : allMatches) {
            String playerA = match.getPlayer1Id();
            String playerB = match.getPlayer2Id();

            String key;
            if (playerB == null) {
                key = String.join("|", playerA, "BYE", match.getTournamentId(), String.valueOf(match.getRound()));
            } else {
                List<String> players = Arrays.asList(playerA, playerB);
                players.sort(Comparator.nullsLast(String::compareTo)); // null en dernier, juste au cas où
                key = String.join("|", players.get(0), players.get(1), match.getTournamentId(), String.valueOf(match.getRound()));
            }

            if (uniqueMatchKeys.contains(key)) {
                toDelete.add(match);
            } else {
                uniqueMatchKeys.add(key);
            }
        }

        toDelete.forEach(match -> matchRepository.deleteById(match.getId()));
        return toDelete.size();
    }



}
