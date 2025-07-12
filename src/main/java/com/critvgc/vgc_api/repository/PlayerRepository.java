package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Player;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PlayerRepository extends MongoRepository<Player, String> {
        Optional<Player> findByFirstNameAndLastNameAndCountry(String firstName, String lastName, String country);
        Optional<Player> findByTrainerName(String trainerName);
}
