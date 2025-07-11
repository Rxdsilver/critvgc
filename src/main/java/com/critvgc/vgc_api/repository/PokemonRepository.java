package com.critvgc.vgc_api.repository;

import com.critvgc.vgc_api.model.Pokemon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PokemonRepository extends JpaRepository<Pokemon, Long> {}
