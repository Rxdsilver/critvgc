package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Match;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchRepository extends MongoRepository<Match, String> {
    Optional<Match> findByRoundAndPlayer1IdAndPlayer2Id(Integer round, String player1Id, String player2Id);
    List<Match> findByPlayer1IdOrPlayer2Id(String player1Id, String player2Id);

}
