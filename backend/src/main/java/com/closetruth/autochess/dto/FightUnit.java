package com.closetruth.autochess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FightUnit {

    private String id;
    private String name;
    private int atk;
    private int maxHp;
    private int currentHp;
    private int tier;
    private String trait;
    private int stars;

    public FightUnit() {
    }

    public static FightUnit fromShop(ShopOffer offer, String id) {
        FightUnit u = new FightUnit();
        u.id = id;
        u.name = offer.getName();
        u.atk = offer.getAtk();
        u.maxHp = offer.getHp();
        u.currentHp = offer.getHp();
        u.tier = offer.getTier();
        u.trait = offer.getTrait();
        u.stars = 1;
        return u;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public void setMaxHp(int maxHp) {
        this.maxHp = maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int currentHp) {
        this.currentHp = currentHp;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
    }

    public String getTrait() {
        return trait == null ? "" : trait;
    }

    public void setTrait(String trait) {
        this.trait = trait;
    }

    public int getStars() {
        return stars <= 0 ? 1 : stars;
    }

    public void setStars(int stars) {
        this.stars = stars;
    }

    public void fullHeal() {
        this.currentHp = this.maxHp;
    }

    /** 用于战斗拼点的基础战力（含星级系数）。 */
    public int combatPower() {
        int base = atk * 2 + Math.max(0, currentHp);
        double starMult = 1.0 + 0.12 * (getStars() - 1);
        return (int) Math.round(base * starMult);
    }
}
