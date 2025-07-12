package com.critvgc.vgc_api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.critvgc.vgc_api.repository.MatchRepository;
import com.critvgc.vgc_api.repository.PlayerRepository;
import com.critvgc.vgc_api.repository.TeamRepository;
import com.critvgc.vgc_api.repository.TournamentRepository;
import com.critvgc.vgc_api.service.MatchDataLoader;

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
	void testImportMatchesForAllCategories() {
		MatchDataLoader matchDataLoader = new MatchDataLoader(tournamentRepository, playerRepository, teamRepository, matchRepository);
		String code = "NA02wFHPyTOSTSFEbwHA";

		try {
			int result = matchDataLoader.importMatchesForAllCategories(code);
			assert(result > 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
