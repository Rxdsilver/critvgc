package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Team;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TeamRepository extends MongoRepository<Team, String> {
    List<Team> findByPlayerId(String playerId);
    List<Team> findByTournamentId(String tournamentId);
    Optional<Team> findByPlayerIdAndTournamentId(String playerId, String tournamentId);
}
