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
            Document doc = Jsoup.connect(rosterUrl).maxBodySize(0).get();;

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
            
            List<Player> masters = new ArrayList<>();
            List<Player> seniors = new ArrayList<>();
            List<Player> juniors = new ArrayList<>();

            Elements rows = doc.select("table tbody tr");

            for (Element row : rows) {
                Elements cells = row.select("td");

                try {
                    String fullname = cells.get(1).text()+" "+cells.get(2).text();
                    String country = cells.get(3).text();
                    String division = cells.get(4).text().toUpperCase();
                    Integer standing = Integer.parseInt(cells.get(7).text());
                    Element link = cells.get(6).selectFirst("a");

                    if (link != null) {
                        String relativeHref = link.attr("href");
                        String teamUrl = "https://rk9.gg" + relativeHref;

                        Player player = playerRepository.findByFullnameAndCountry(fullname, country)
                                .orElseGet(() -> {
                                    Player p = new Player();
                                    p.setFullname(fullname);
                                    p.setDivision(division);
                                    p.setCountry(country);
                                    return playerRepository.save(p);
                                    
                                });

                        switch (division) {
                            case "MASTERS" -> insertAtRank(masters, standing, player);
                            case "SENIOR" -> insertAtRank(seniors, standing, player);
                            case "JUNIOR" -> insertAtRank(juniors, standing, player);
                        }

                        Team team = parseTeam(teamUrl);
                        if (team != null) {
                            team.setPlayerId(player.getId());
                            team.setPlayerName(player.getFullname());
                            team.setTournamentId(tournament.getId());
                            team.setTournamentName(tournament.getName());

                            teamRepository.findByPlayerIdAndTournamentId(player.getId(), tournament.getId())
                                    .stream().findFirst().ifPresentOrElse(existingTeam -> {
                                        // Update existing team
                                        existingTeam.setPokemons(team.getPokemons());
                                        teamRepository.save(existingTeam);
                                    }, () -> {
                                        // Save new team
                                        teamRepository.save(team);
                                    });
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    System.err.println("Skipping row due to missing data: " + e.getMessage()
                            + " in row: " + row.text());
                    continue;
                }
                
            }
            tournament.setMasters(masters);
            tournament.setSeniors(seniors);
            tournament.setJuniors(juniors);
            tournamentRepository.save(tournament);
            matchDataLoader.importMatchesForAllCategories(code);

            System.out.println("Tournament import complete.");
            return true;

        } catch (Exception e) {
            System.err.println("Error importing tournament: " + e.getMessage());
            return false;
        }
    }

    public Team parseTeam(String teamUrl) {
        try {
            Document doc = Jsoup.connect(teamUrl).get();
            Elements pokeDivs = doc.select("div#lang-EN div.pokemon.bg-light-green-50.p-3");

            List<Pokemon> pokemons = new ArrayList<>();

            for (Element pokemonDiv : pokeDivs) {
            Pokemon poke = new Pokemon();

            String wholeText = pokemonDiv.wholeText();

            String name = wholeText.split("\\bTera Type:")[0].trim().split("\n")[0];

            String type = pokemonDiv.select("b:contains(Tera Type:)").first().nextSibling().toString().trim();
            String ability = pokemonDiv.select("b:contains(Ability:)").first().nextSibling().toString().replace("&nbsp;", "").trim();
            String item = pokemonDiv.select("b:contains(Held Item:)").first().nextSibling().toString().trim();

            List<String> moves = new ArrayList<>();
            Elements moveElements = pokemonDiv.select("h5 span.badge");

            for (Element moveElement : moveElements) {
                moves.add(moveElement.text());
            }
            poke.setName(name);
            poke.setTeraType(type);
            poke.setAbility(ability);
            poke.setItem(item);
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


    static public String detectRegionFromVenue(String code) {
    try {
        String venueUrl = "https://rk9.gg/tournament/" + code;
        Document doc = Jsoup.connect(venueUrl).get();

        // Trouve tous les <dt>
        Elements dtElements = doc.select("dt");

        for (Element dt : dtElements) {
            if (dt.text().trim().equalsIgnoreCase("Venue")) {
                Element dd = dt.nextElementSibling(); // <dd> associé
                if (dd != null && dd.tagName().equals("dd")) {
                    String[] lines = dd.html().split("<br>");
                    for (String line : lines) {
                        String clean = Jsoup.parse(line).text().trim();
                        // Cherche ligne avec virgule et état (ex: New Orleans, LA 70130)
                        if (clean.matches(".*, [A-Z]{2} \\d{5}")) {
                            // Extrait la ville : "New Orleans" dans "New Orleans, LA 70130"
                            String city = clean.split(",")[0].trim();
                            if (!city.isEmpty()) {
                                return fetchRegionFromCity(city);
                            }
                        }
                    }
                }
            }
        }
    } catch (Exception e) {
        System.err.println("Error detecting region from venue: " + e.getMessage());
    }
    return "Unknown";
}


    public static String fetchRegionFromCity(String city) {
        try {
            String url = "https://nominatim.openstreetmap.org/search?q=" + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&format=json&limit=1&addressdetails=1";

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.add("User-Agent", "vgc-api/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            if (root.isArray() && root.size() > 0) {
                JsonNode address = root.get(0).get("address");

                if (address.has("country")) {
                    String country = address.get("country").asText();
                    return mapCountryToRegion(country);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching region from city: " + e.getMessage());
        }
        return "Unknown";
    }


    private static String mapCountryToRegion(String country) {
        switch (country.toLowerCase()) {
            case "united states":
            case "canada":
            case "mexico":
                return "NA";
            case "france":
            case "germany":
            case "spain":
            case "united kingdom":
            case "italy":
            case "netherlands":
                return "EU";
            case "brazil":
            case "argentina":
            case "chile":
                return "LAT";
            case "australia":
            case "new zealand":
                return "OCE";
            case "south africa":
            case "nigeria":
                return "AFR";
            default:
                return "Unknown";
        }
    }

    private void insertAtRank(List<Player> list, int rank, Player player) {
    int index = rank - 1;
    while (list.size() <= index) {
        list.add(null); // remplissage pour atteindre la bonne taille
    }
    list.set(index, player);
}

}
