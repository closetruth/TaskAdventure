package com.closetruth.autochess;

import com.closetruth.autochess.dto.AutochessGameResponse;
import com.closetruth.autochess.dto.BoardIndexRequest;
import com.closetruth.autochess.dto.PlaceRequest;
import com.closetruth.autochess.dto.SlotRequest;
import com.closetruth.autochess.dto.UnitIdRequest;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class AutochessController {

    private final AutochessGameService autochessGameService;

    public AutochessController(AutochessGameService autochessGameService) {
        this.autochessGameService = autochessGameService;
    }

    @GetMapping("/autochess/game")
    public AutochessGameResponse game() {
        return autochessGameService.getGame();
    }

    @PostMapping("/autochess/shop/refresh")
    public AutochessGameResponse refreshShop() {
        return autochessGameService.refreshShop();
    }

    @PostMapping("/autochess/shop/buy")
    public AutochessGameResponse buy(@RequestBody SlotRequest body) {
        return autochessGameService.buy(body);
    }

    @PostMapping("/autochess/unit/place")
    public AutochessGameResponse place(@RequestBody PlaceRequest body) {
        return autochessGameService.place(body);
    }

    @PostMapping("/autochess/unit/unplace")
    public AutochessGameResponse unplace(@RequestBody BoardIndexRequest body) {
        return autochessGameService.unplace(body);
    }

    @PostMapping("/autochess/unit/sell")
    public AutochessGameResponse sell(@RequestBody UnitIdRequest body) {
        return autochessGameService.sell(body);
    }

    @PostMapping("/autochess/unit/upgrade")
    public AutochessGameResponse upgrade(@RequestBody UnitIdRequest body) {
        return autochessGameService.upgrade(body);
    }

    @PostMapping("/autochess/unit/merge")
    public AutochessGameResponse merge(@RequestBody UnitIdRequest body) {
        return autochessGameService.merge(body);
    }

    @PostMapping("/autochess/round/fight")
    public AutochessGameResponse fight() {
        return autochessGameService.fight();
    }

    @PostMapping("/autochess/run/reset")
    public AutochessGameResponse resetRun() {
        return autochessGameService.resetRun();
    }

    @PostMapping("/autochess/run/revive")
    public AutochessGameResponse revive() {
        return autochessGameService.revive();
    }
}
