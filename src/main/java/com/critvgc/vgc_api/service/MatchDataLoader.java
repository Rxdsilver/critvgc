package com.critvgc.vgc_api.service;

import com.critvgc.vgc_api.model.Match;
import com.critvgc.vgc_api.model.Player;
import com.critvgc.vgc_api.model.Team;
import com.critvgc.vgc_api.model.Tournament;
import com.critvgc.vgc_api.repository.MatchRepository;
import com.critvgc.vgc_api.repository.PlayerRepository;
import com.critvgc.vgc_api.repository.TeamRepository;
import com.critvgc.vgc_api.repository.TournamentRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchDataLoader {

    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;

    public MatchDataLoader(TournamentRepository tournamentRepository,
                           PlayerRepository playerRepository,
                           TeamRepository teamRepository,
                           MatchRepository matchRepository) {
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.matchRepository = matchRepository;
    }

    public boolean importMatches(String code) {
        try {
            Optional<Tournament> optTournament = tournamentRepository.findByCode(code);
            if (optTournament.isEmpty()) {
                System.err.println("Tournament with code " + code + " not found.");
                return false;
            }

            Tournament tournament = optTournament.get();
            String pairingsUrl = "https://rk9.gg/pairings/" + code;
            Document doc = Jsoup.connect(pairingsUrl).get();

            Elements matchRows = doc.select("table tbody tr");
            int imported = 0;

            for (Element row : matchRows) {
                Elements cells = row.select("td");
                if (cells.size() < 5) continue;

                String player1Name = cells.get(1).text().trim();
                String player2Name = cells.get(2).text().trim();
                String result = cells.get(3).text().trim();

                Optional<Player> optP1 = findPlayerFromDisplayName(player1Name);
                Optional<Player> optP2 = findPlayerFromDisplayName(player2Name);

                if (optP1.isEmpty() || optP2.isEmpty()) {
                    System.out.println("Skipping match, player not found: " + player1Name + " vs " + player2Name);
                    continue;
                }

                Player p1 = optP1.get();
                Player p2 = optP2.get();

                Match match = new Match();
                match.setTournamentId(tournament.getId());

                match.setPlayer1Id(p1.getId());
                match.setPlayer2Id(p2.getId());

                Optional<Team> team1 = teamRepository.findByPlayerIdAndTournamentId(p1.getId(), tournament.getId());
                Optional<Team> team2 = teamRepository.findByPlayerIdAndTournamentId(p2.getId(), tournament.getId());

                team1.ifPresent(t -> match.setTeam1Id(t.getId()));
                team2.ifPresent(t -> match.setTeam2Id(t.getId()));

                // Déterminer le gagnant
                if (result.equals("W")) {
                    match.setWinnerId(p1.getId());
                } else if (result.equals("L")) {
                    match.setWinnerId(p2.getId());
                } else {
                    // Résultat inconnu ou match nul
                    match.setWinnerId(null);
                }

                matchRepository.save(match);
                imported++;
            }

            System.out.println("Match import complete: " + imported + " matches.");
            return true;

        } catch (Exception e) {
            System.err.println("Error importing matches: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private Optional<Player> findPlayerFromDisplayName(String displayName) {
        if (!displayName.contains("[") || !displayName.contains("]")) return Optional.empty();

        String country = displayName.substring(displayName.indexOf('[') + 1, displayName.indexOf(']')).trim();
        String fullName = displayName.substring(0, displayName.indexOf('[')).trim();

        String[] nameParts = fullName.split(" ");
        if (nameParts.length < 2) return Optional.empty();

        String firstName = nameParts[0].trim();
        String lastName = String.join(" ", Arrays.copyOfRange(nameParts, 1, nameParts.length)).trim();

        return playerRepository.findByFirstNameAndLastNameAndCountry(firstName, lastName, country);
    }

}
