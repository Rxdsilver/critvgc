package com.critvgc.vgc_api.service;

import com.critvgc.vgc_api.model.Match;
import com.critvgc.vgc_api.model.Player;
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

    public int importMatchesForAllCategories(String code) {

        try {
            Optional<Tournament> optTournament = tournamentRepository.findByCode(code);
            if (optTournament.isEmpty()) {
                System.err.println("Tournament with code " + code + " not found.");
                return -1;
            }

            Tournament tournament = optTournament.get();
            Map<String, Integer> roundCounts = fetchCategoryRoundCounts(code);
            Map<String, Integer> pods = Map.of("masters", 2, "senior", 9, "junior", 0);

            int totalImported = 0;
            for (String category : roundCounts.keySet()) {
                int pod = pods.getOrDefault(category, 2);
                int rounds = roundCounts.get(category);

                for (int round = 1; round <= rounds; round++) {
                    String url = "https://rk9.gg/pairings/" + code + "?pod=" + pod + "&rnd=" + round;
                    Document doc = Jsoup.connect(url).get();
                    Elements matches = doc.select("div.row.row-cols-3.match.no-gutter.complete");

                    for (Element match : matches) {
                        Element player1Div = match.selectFirst("div.player1");
                        Element player2Div = match.selectFirst("div.player2");

                        String player1Name = player1Div.select("span.name").text().trim();
                        String player2Name = player2Div.select("span.name").text().trim();

                        boolean isBye = player2Name.isEmpty();

                        Optional<Player> optP1 = findPlayerFromDisplayName(player1Name);
                        if (optP1.isEmpty()) {
                            // System.out.println("Skipping match, player 1 not found: " + player1Name);
                            continue;
                        }

                        Player p1 = optP1.get();
                        Match m = new Match();
                        m.setTournamentId(tournament.getId());
                        m.setPlayer1Id(p1.getId());
                        m.setPlayer1Name(p1.getFullname());
                        m.setRound(round);

                        teamRepository.findByPlayerIdAndTournamentId(p1.getId(), tournament.getId())
                                .stream().findFirst().ifPresent(t -> m.setTeam1Id(t.getId()));

                        if (isBye) {
                            // Match avec un seul joueur
                            m.setBye(true);
                            m.setMatchStatus("P1_WIN");
                        } else {
                            Optional<Player> optP2 = findPlayerFromDisplayName(player2Name);
                            if (optP2.isEmpty()) {
                                // System.out.println("Skipping match, player 2 not found: " + player2Name);
                                continue;
                            }

                            Player p2 = optP2.get();
                            m.setPlayer2Id(p2.getId());
                            m.setPlayer2Name(p2.getFullname());
                            teamRepository.findByPlayerIdAndTournamentId(p2.getId(), tournament.getId())
                                    .stream().findFirst().ifPresent(t -> m.setTeam2Id(t.getId()));

                            boolean player1IsWinner = player1Div.hasClass("winner");
                            boolean player2IsWinner = player2Div.hasClass("winner");
                            boolean isTie1 = player1Div.hasClass("tie");
                            boolean isTie2 = player2Div.hasClass("tie");

                            if (player1IsWinner) m.setMatchStatus("P1_WIN");
                            else if (player2IsWinner) m.setMatchStatus("P2_WIN");
                            else if (isTie1 && isTie2) m.setMatchStatus("TIE");
                        }

                        matchRepository.findByRoundAndPlayer1IdAndPlayer2Id(
                                m.getRound(), m.getPlayer1Id(), m.getPlayer2Id())
                                .ifPresentOrElse(existingMatch -> {
                                    // Update existing match
                                    existingMatch.setMatchStatus(m.getMatchStatus());
                                    matchRepository.save(existingMatch);
                                }, () -> {
                                    // Save new match
                                    matchRepository.save(m);
                                });
                        totalImported++;
                    }
                }
            }

            System.out.println("Imported matches: " + totalImported);
            return totalImported;

        } catch (Exception e) {
            System.err.println("Error importing matches: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }


    public Map<String, Integer> fetchCategoryRoundCounts(String code) throws Exception {
        Map<String, Integer> roundCounts = new HashMap<>();
        Document doc = Jsoup.connect("https://rk9.gg/pairings/" + code).get();
        Elements links = doc.select("ul.nav.nav-pills li a");

        for (Element link : links) {
            String text = link.text().trim().toLowerCase();
            // System.out.println("Processing link: " + text);
            if (text.matches("(masters|senior|junior) in round \\d+")) {
                String[] parts = text.split(" ");
                String category = parts[0];
                int round = Integer.parseInt(parts[3]);
                roundCounts.merge(category, round, Integer::max);
            }
        }
        return roundCounts;
    }

    private Optional<Player> findPlayerFromDisplayName(String displayName) {
        if (!displayName.contains("[") || !displayName.contains("]")) return Optional.empty();

        String country = displayName.substring(displayName.indexOf('[') + 1, displayName.indexOf(']')).trim();
        String fullName = displayName.substring(0, displayName.indexOf('[')).trim();

        return playerRepository.findByFullnameAndCountry(fullName, country);
    }
}
