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
    private String fullname;
    private String country;
    private String division;
}

