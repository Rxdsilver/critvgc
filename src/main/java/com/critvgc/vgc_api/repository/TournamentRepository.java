package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Tournament;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TournamentRepository extends MongoRepository<Tournament, String> {

    Optional<Tournament> findByName(String name);
    Optional<Tournament> findByCode(String code);
}
