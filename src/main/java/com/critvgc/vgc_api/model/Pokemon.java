package com.critvgc.vgc_api.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class Pokemon {
    private String name;
    private String item;
    private String ability;
    private String teraType;
    private List<String> moves; // liste de noms de capacités
}

