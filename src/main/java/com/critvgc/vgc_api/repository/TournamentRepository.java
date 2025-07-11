package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {}
