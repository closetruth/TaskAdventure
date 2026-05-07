package com.closetruth.season;

import com.closetruth.game.GameActionResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @GetMapping("/season/week")
    public WeekSummaryResponse week() {
        return seasonService.currentWeekSummary();
    }

    @PostMapping("/season/week/claim")
    public GameActionResponse claimWeek() {
        return seasonService.claimCurrentWeek();
    }
}
