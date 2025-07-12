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
                        if (player1Div == null || player2Div == null) continue;

                        Element name1El = player1Div.selectFirst("span.name");
                        Element name2El = player2Div.selectFirst("span.name");
                        if (name1El == null || name2El == null) {
                            System.out.println("Skipping match, missing player name span.");
                            continue;
                        }

                        String name1 = name1El.text().trim();
                        String name2 = name2El.text().trim();

                        Optional<Player> optP1 = findPlayerFromDisplayName(name1);
                        Optional<Player> optP2 = findPlayerFromDisplayName(name2);

                        if (optP1.isEmpty() || optP2.isEmpty()) {
                            System.out.println("Skipping match, player not found: " + name1 + " vs " + name2);
                            continue;
                        }

                        Player p1 = optP1.get();
                        Player p2 = optP2.get();

                        Match m = new Match();
                        m.setTournamentId(tournament.getId());
                        m.setPlayer1Id(p1.getId());
                        m.setPlayer2Id(p2.getId());

                        teamRepository.findByPlayerIdAndTournamentId(p1.getId(), tournament.getId())
                                .stream().findFirst().ifPresent(t -> m.setTeam1Id(t.getId()));
                        teamRepository.findByPlayerIdAndTournamentId(p2.getId(), tournament.getId())
                                .stream().findFirst().ifPresent(t -> m.setTeam2Id(t.getId()));

                        boolean p1Win = player1Div.classNames().contains("winner");
                        boolean p2Win = player2Div.classNames().contains("winner");
                        boolean tie = player1Div.classNames().contains("tie") && player2Div.classNames().contains("tie");

                        if (p1Win) m.setWinnerId(p1.getId());
                        else if (p2Win) m.setWinnerId(p2.getId());
                        else if (tie) m.setWinnerId(null);

                        matchRepository.save(m);
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
