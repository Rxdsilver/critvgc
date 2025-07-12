package com.critvgc.vgc_api.model;

import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.Setter;

@Document(collection = "tournaments")
@Getter @Setter
public class Tournament {
    @Id
    private String id;
    private String name;
    private String location;
    private LocalDate date;
    private String code;
}


