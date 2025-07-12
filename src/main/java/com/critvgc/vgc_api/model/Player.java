package com.critvgc.vgc_api.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "players")
@Getter @Setter
public class Player {
    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String trainerName;
    private String country;
    private String division;
}

