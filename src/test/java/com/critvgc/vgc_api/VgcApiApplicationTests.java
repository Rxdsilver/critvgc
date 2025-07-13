package com.critvgc.vgc_api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.critvgc.vgc_api.model.Team;
import com.critvgc.vgc_api.repository.MatchRepository;
import com.critvgc.vgc_api.repository.PlayerRepository;
import com.critvgc.vgc_api.repository.TeamRepository;
import com.critvgc.vgc_api.repository.TournamentRepository;
import com.critvgc.vgc_api.service.MatchDataLoader;
import com.critvgc.vgc_api.service.TournamentDataLoader;

@SpringBootTest
class VgcApiApplicationTests {

	TournamentRepository tournamentRepository;
	PlayerRepository playerRepository;
	MatchRepository matchRepository;
	TeamRepository teamRepository;

	@Test
	void contextLoads() {
	}

	@Test
	void testFetchCategoryRoundCounts(){
		MatchDataLoader matchDataLoader = new MatchDataLoader(tournamentRepository, playerRepository, teamRepository, matchRepository);
		String code = "NA02wFHPyTOSTSFEbwHA";

		try {
			Map<String, Integer> roundCounts = matchDataLoader.fetchCategoryRoundCounts(code);

			assertEquals(roundCounts.get("masters"), 17);
			assertEquals(roundCounts.get("senior"), 12);
			assertEquals(roundCounts.get("junior"), 11);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	void testParseTeam() {
		TournamentDataLoader tournamentDataLoader = new TournamentDataLoader(tournamentRepository, playerRepository, teamRepository, null);
		String teamUrl = "https://rk9.gg/teamlist/public/NA02wFHPyTOSTSFEbwHA/ye0VJqAnvFnw8oLUGZEB";

		String pokeString1 = "Miraidon @ Choice Scarf\n" +
				"Tera Type: Electric\n" +
				"Ability: Hadron Engine\n" +
				"- Electro Drift\n" +
				"- Draco Meteor\n" +
				"- Snarl\n" +
				"- Volt Switch";

		String pokeString2 = "Volcarona @ Electric Seed\n" +
				"Tera Type: Grass\n" +
				"Ability: Flame Body\n" +
				"- Protect\n" +
				"- Quiver Dance\n" +
				"- Fiery Dance\n" +
				"- Giga Drain";

		try {
			Team team = tournamentDataLoader.parseTeam(teamUrl);
			assertEquals(pokeString1, team.getPokemons().get(0).toString());
			assertEquals(pokeString2, team.getPokemons().get(1).toString());
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	void testDetectRegionFromVenue(){
		String code = "NA02wFHPyTOSTSFEbwHA";
		String region = null;
		try {
			region = TournamentDataLoader.detectRegionFromVenue(code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals("NA", region);
	}

	@Test
	void testFetchRegionFromCity(){
		String city = "New Orleans";
		assertEquals("NA", TournamentDataLoader.fetchRegionFromCity(city));
	}

}
