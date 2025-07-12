package com.critvgc.vgc_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.critvgc.vgc_api.model.Player;

public interface PlayerRepository extends MongoRepository<Player, String> {

    Optional<Player> findByFirstNameAndLastNameAndCountry(String firstName, String lastName, String country);

    @Query("{ 'lastName': { $regex: ?0, $options: 'i' }, 'country': ?1 }")
    List<Player> findByLastNameRegexAndCountry(String lastNamePattern, String country);
}
