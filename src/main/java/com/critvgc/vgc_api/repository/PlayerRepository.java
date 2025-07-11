package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {}
