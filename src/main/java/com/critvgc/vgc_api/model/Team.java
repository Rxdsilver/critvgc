package com.critvgc.vgc_api.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "teams")
@Getter @Setter
public class Team {
    @Id
    private String id;
    private String playerId;
    private String playerName;  
    private String tournamentId;
    private String tournamentName;
    private List<Pokemon> pokemons;
}

