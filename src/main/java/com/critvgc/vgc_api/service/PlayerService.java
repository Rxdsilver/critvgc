package com.critvgc.vgc_api.service;

import com.critvgc.vgc_api.model.Player;
import com.critvgc.vgc_api.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;

    /**
     * Recherche insensible aux accents et à la casse.
     */
    public Optional<Player> findInsensitiveByFullnameAndCountry(String fullname, String country) {
        List<Player> players = playerRepository.findByCountry(country);
        String normalizedTarget = normalize(fullname);

        return players.stream()
            .filter(p -> normalize(p.getFullname()).equals(normalizedTarget))
            .findFirst();
    }

    /**
     * Normalise une chaîne (supprime accents, met en minuscules).
     */
    private String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                         .replaceAll("\\p{M}", "")
                         .toLowerCase();
    }

    /**
     * Met une majuscule à la première lettre de chaque mot.
     */
    private String capitalize(String input) {
        if (input == null || input.isBlank()) return input;

        return Arrays.stream(input.trim().toLowerCase().split("\\s+"))
            .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1))
            .collect(Collectors.joining(" "));
    }

    /**
     * Uniformise les noms dans la base (capitalisation).
     */
    public void uniformizeFullnamesInDatabase() {
        List<Player> players = playerRepository.findAll();
        int count = 0;

        for (Player player : players) {
            String original = player.getFullname();
            String capitalized = capitalize(original);

            if (!capitalized.equals(original)) {
                player.setFullname(capitalized);
                playerRepository.save(player);
                count++;
            }
        }

        System.out.println("Updated " + count + " players with capitalized fullnames.");
    }
}
