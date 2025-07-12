package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Match;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MatchRepository extends MongoRepository<Match, String> {}
