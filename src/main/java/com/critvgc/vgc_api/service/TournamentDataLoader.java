package com.critvgc.vgc_api.service;

import com.critvgc.vgc_api.model.*;
import com.critvgc.vgc_api.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class TournamentDataLoader {

    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;
    private final MatchDataLoader matchDataLoader;

    public TournamentDataLoader(TournamentRepository tournamentRepository,
                                 PlayerRepository playerRepository,
                                 TeamRepository teamRepository,
                                 MatchDataLoader matchDataLoader) {
        this.tournamentRepository = tournamentRepository;
        this.playerRepository = playerRepository;
        this.teamRepository = teamRepository;
        this.matchDataLoader = matchDataLoader;
    }

    public boolean importOrUpdateTournament(String code) {
        try {
            String rosterUrl = "https://rk9.gg/roster/" + code;
            Document doc = Jsoup.connect(rosterUrl).get();

            Element nameElement = doc.selectFirst("div.pt-4 h4");
            String tournamentName = nameElement != null ? nameElement.text().trim() : "Unnamed Tournament";

            Element dateElement = doc.selectFirst("div.d-flex.justify-content-end h5");
            LocalDate tournamentDate = LocalDate.now();
            if (dateElement != null) {
                String dateText = dateElement.ownText().trim();
                dateText = dateText.split(" at ")[0];
                tournamentDate = LocalDate.parse(dateText, DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));
            }

            String location = detectRegionFromVenue(code);

            Tournament tournament = tournamentRepository.findByCode(code).orElseGet(() -> {
                Tournament newT = new Tournament();
                newT.setCode(code);
                return newT;
            });

            tournament.setName(tournamentName);
            tournament.setDate(tournamentDate);
            tournament.setLocation(location);
            tournamentRepository.save(tournament);

            Elements rows = doc.select("table#dtLiveRoster tbody tr");

            for (Element row : rows) {
                Elements cells = row.select("td");
                if (cells.size() < 8) continue;

                String firstName = cells.get(1).text();
                String lastName = cells.get(2).text();
                String country = cells.get(3).text();
                String division = cells.get(4).text().toUpperCase();
                String trainerName = cells.get(5).text();
                Element link = cells.get(6).selectFirst("a");

                if (link != null) {
                    String relativeHref = link.attr("href");
                    String teamUrl = "https://rk9.gg" + relativeHref;

                    Player player = playerRepository.findByFirstNameAndLastNameAndCountry(firstName, lastName, country)
                            .orElseGet(() -> {
                                Player p = new Player();
                                p.setFirstName(firstName);
                                p.setLastName(lastName);
                                p.setDivision(division);
                                p.setCountry(country);
                                p.setTrainerName(trainerName);
                                playerRepository.save(p);
                                return p;
                            });

                    Team team = parseTeam(teamUrl);
                    if (team != null) {
                        team.setPlayerId(player.getId());
                        team.setTournamentId(tournament.getId());
                        teamRepository.save(team);
                    }
                }
            }

            matchDataLoader.importMatches(code);

            System.out.println("Tournament import complete.");
            return true;

        } catch (Exception e) {
            System.err.println("Error importing tournament: " + e.getMessage());
            return false;
        }
    }

    private Team parseTeam(String teamUrl) {
        try {
            Document doc = Jsoup.connect(teamUrl).get();
            Elements pokeDivs = doc.select("div.team-pokemon");

            List<Pokemon> pokemons = new ArrayList<>();

            for (Element pokeDiv : pokeDivs) {
                Pokemon poke = new Pokemon();
                poke.setName(getTextOrEmpty(pokeDiv, ".poke-name"));
                poke.setTeraType(getTextOrEmpty(pokeDiv, ".poke-tera"));
                poke.setItem(getTextOrEmpty(pokeDiv, ".poke-item"));
                poke.setAbility(getTextOrEmpty(pokeDiv, ".poke-ability"));

                List<String> moves = new ArrayList<>();
                Elements moveElements = pokeDiv.select(".poke-moves li");
                for (Element moveEl : moveElements) {
                    String move = moveEl.text().trim();
                    if (!move.isEmpty())
                        moves.add(move);
                }
                poke.setMoves(moves);
                pokemons.add(poke);
            }

            Team team = new Team();
            team.setPokemons(pokemons);
            return team;

        } catch (IOException e) {
            System.err.println("Error parsing team: " + e.getMessage());
            return null;
        }
    }

    private String getTextOrEmpty(Element parent, String cssQuery) {
        Element el = parent.selectFirst(cssQuery);
        return el != null ? el.text() : "";
    }

    private String detectRegionFromVenue(String code) {
        try {
            String venueUrl = "https://rk9.gg/tournament/" + code;
            Document doc = Jsoup.connect(venueUrl).get();

            Element venueElement = doc.selectFirst("div.card-body address");
            if (venueElement != null) {
                String fullAddress = venueElement.text();
                String[] parts = fullAddress.split(",");
                String city = parts.length > 0 ? parts[0].trim() : "";

                if (!city.isEmpty()) {
                    return fetchRegionFromCity(city);
                }
            }
        } catch (Exception e) {
            System.err.println("Error detecting region from venue: " + e.getMessage());
        }
        return "Unknown";
    }

    private String fetchRegionFromCity(String city) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&format=json&limit=1";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "vgc-api/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            if (root.isArray() && root.size() > 0) {
                JsonNode address = root.get(0).get("address");
                String continent = address.get("continent").asText();
                return mapContinentToRegion(continent);
            }
        } catch (Exception e) {
            System.err.println("Error fetching region from city: " + e.getMessage());
        }
        return "Unknown";
    }

    private String mapContinentToRegion(String continent) {
        switch (continent.toLowerCase()) {
            case "europe": return "EU";
            case "north america": return "NA";
            case "south america": return "LAT";
            case "oceania": return "OCE";
            case "africa": return "AFR";
            default: return "Unknown";
        }
    }
}
