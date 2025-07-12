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
import java.util.stream.Collectors;

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
            List<Integer> rounds = fetchAvailableRounds(code);
            System.out.println("Found rounds: " + rounds);

            int imported = 0;

            for (int round : rounds) {
                String roundUrl = "https://rk9.gg/pairings/" + code + "?pod=2&round=" + round;
                Document doc = Jsoup.connect(roundUrl).get();

                Elements matchDivs = doc.select("div.row.row-cols-3.match.no-gutter.complete");

                for (Element matchDiv : matchDivs) {
                    Element p1Div = matchDiv.selectFirst("div.player1");
                    Element p2Div = matchDiv.selectFirst("div.player2");

                    String name1 = p1Div.selectFirst("span.name") != null ? p1Div.selectFirst("span.name").text().trim() : null;
                    String name2 = p2Div.selectFirst("span.name") != null ? p2Div.selectFirst("span.name").text().trim() : null;

                    if (name1 == null || name2 == null) continue;

                    System.out.println("Recherche joueur?: " + name1);
                    System.out.println("Recherche joueur?: " + name2);

                    Optional<Player> optP1 = findPlayerFromDisplayName(name1);
                    Optional<Player> optP2 = findPlayerFromDisplayName(name2);

                    if (optP1.isEmpty() || optP2.isEmpty()) {
                        System.out.println("Skipping match, player not found: " + name1 + " vs " + name2);
                        continue;
                    }

                    Player p1 = optP1.get();
                    Player p2 = optP2.get();

                    Match match = new Match();
                    match.setTournamentId(tournament.getId());
                    match.setPlayer1Id(p1.getId());
                    match.setPlayer2Id(p2.getId());

                    // Gérer les équipes avec findAll
                    List<Team> teams1 = teamRepository.findByPlayerId(p1.getId()).stream()
                            .filter(t -> t.getTournamentId().equals(tournament.getId()))
                            .collect(Collectors.toList());

                    List<Team> teams2 = teamRepository.findByPlayerId(p2.getId()).stream()
                            .filter(t -> t.getTournamentId().equals(tournament.getId()))
                            .collect(Collectors.toList());

                    if (teams1.size() > 1) System.out.println("?? Plusieurs équipes trouvées pour " + name1);
                    if (teams2.size() > 1) System.out.println("?? Plusieurs équipes trouvées pour " + name2);

                    teams1.stream().findFirst().ifPresent(t -> match.setTeam1Id(t.getId()));
                    teams2.stream().findFirst().ifPresent(t -> match.setTeam2Id(t.getId()));

                    boolean player1Won = p1Div.classNames().contains("winner");
                    boolean player2Won = p2Div.classNames().contains("winner");
                    boolean tie = p1Div.classNames().contains("tie") && p2Div.classNames().contains("tie");

                    if (player1Won) match.setWinnerId(p1.getId());
                    else if (player2Won) match.setWinnerId(p2.getId());
                    else if (tie) match.setWinnerId(null); // Match nul
                    else match.setWinnerId(null); // En cours ou erreur

                    matchRepository.save(match);
                    imported++;
                }
            }

            System.out.println("? Match import complete: " + imported + " matches.");
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

    private List<Integer> fetchAvailableRounds(String code) throws Exception {
        Document doc = Jsoup.connect("https://rk9.gg/pairings/" + code).get();
        Elements roundLinks = doc.select("ul.nav.nav-pills li a");

        Set<Integer> rounds = new TreeSet<>();
        for (Element link : roundLinks) {
            String text = link.text().toLowerCase();
            if (text.contains("masters") && text.matches(".*\\d+")) {
                String number = text.replaceAll("\\D+", "");
                try {
                    rounds.add(Integer.parseInt(number));
                } catch (NumberFormatException ignored) {}
            }
        }
        return new ArrayList<>(rounds);
    }
}
