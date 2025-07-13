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
    private List<String> moves;

    public Pokemon() {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (item != null && !item.isEmpty()) {
            sb.append(" @ ").append(item);
        }
        if (teraType != null && !teraType.isEmpty()) {
            sb.append("\nTera Type: ").append(teraType);
        }
        if (ability != null && !ability.isEmpty()) {
            sb.append("\nAbility: ").append(ability);
        }
        if (moves != null && !moves.isEmpty()) {
            for (String move : moves) {
                sb.append("\n- ").append(move);
            }
        }
        
        return sb.toString();
    }
}

