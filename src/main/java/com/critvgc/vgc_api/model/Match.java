package com.critvgc.vgc_api.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "matches")
@Getter @Setter
public class Match {
    @Id
    private String id;

    private String tournamentId;
    private boolean isBye;
    private String player1Id;
    private String player2Id;

    private String team1Id;
    private String team2Id;  

    private String matchStatus ;
}
