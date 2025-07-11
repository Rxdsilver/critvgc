package com.critvgc.vgc_api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Enumerated(EnumType.STRING)
    private PlayerCategory category;

    @ManyToOne
    private Tournament tournament;

    @OneToOne(mappedBy = "player", cascade = CascadeType.ALL)
    private Team team;

}
