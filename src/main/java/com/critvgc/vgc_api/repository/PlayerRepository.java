package com.critvgc.vgc_api.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.critvgc.vgc_api.model.Player;

public interface PlayerRepository extends MongoRepository<Player, String> {

    Optional<Player> findByFullnameAndCountry(String fullname, String country);
    List<Player> findByFullname(String fullname);

    List<Player> findByCountry(String country);
}
