package com.closetruth.autochess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AutochessPayload {

    private int round;
    private int playerHp;
    private boolean gameOver;
    private List<ShopOffer> shop;
    private List<FightUnit> bench;
    private List<FightUnit> board;
    private String lastLog;

    private int winStreak;
    private int loseStreak;
    private String nextEnemyName;
    private int nextEnemyPowerBase;
    private String synergySummary;

    public AutochessPayload() {
    }

    public static AutochessPayload newRun() {
        AutochessPayload p = new AutochessPayload();
        p.round = 1;
        p.playerHp = 100;
        p.gameOver = false;
        p.winStreak = 0;
        p.loseStreak = 0;
        p.bench = new ArrayList<>();
        p.board = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            p.board.add(null);
        }
        p.shop = new ArrayList<>();
        p.lastLog = "新的一局：上阵棋子（最多 4 格），注意<strong>同羁绊</strong>≥2 有加成；备战席可<strong>合成升星</strong>（需同名同星级×2）。金币来自任务与战斗。";
        return p;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public int getPlayerHp() {
        return playerHp;
    }

    public void setPlayerHp(int playerHp) {
        this.playerHp = playerHp;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void setGameOver(boolean gameOver) {
        this.gameOver = gameOver;
    }

    public List<ShopOffer> getShop() {
        return shop;
    }

    public void setShop(List<ShopOffer> shop) {
        this.shop = shop;
    }

    public List<FightUnit> getBench() {
        return bench;
    }

    public void setBench(List<FightUnit> bench) {
        this.bench = bench;
    }

    public List<FightUnit> getBoard() {
        return board;
    }

    public void setBoard(List<FightUnit> board) {
        this.board = board;
    }

    public String getLastLog() {
        return lastLog;
    }

    public void setLastLog(String lastLog) {
        this.lastLog = lastLog;
    }

    public int getWinStreak() {
        return winStreak;
    }

    public void setWinStreak(int winStreak) {
        this.winStreak = winStreak;
    }

    public int getLoseStreak() {
        return loseStreak;
    }

    public void setLoseStreak(int loseStreak) {
        this.loseStreak = loseStreak;
    }

    public String getNextEnemyName() {
        return nextEnemyName;
    }

    public void setNextEnemyName(String nextEnemyName) {
        this.nextEnemyName = nextEnemyName;
    }

    public int getNextEnemyPowerBase() {
        return nextEnemyPowerBase;
    }

    public void setNextEnemyPowerBase(int nextEnemyPowerBase) {
        this.nextEnemyPowerBase = nextEnemyPowerBase;
    }

    public String getSynergySummary() {
        return synergySummary;
    }

    public void setSynergySummary(String synergySummary) {
        this.synergySummary = synergySummary;
    }
}
