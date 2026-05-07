package com.closetruth.autochess.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShopOffer {

    private int slot;
    private String name;
    private int cost;
    private int atk;
    private int hp;
    private int tier;
    private String trait;

    public ShopOffer() {
    }

    public ShopOffer(int slot, String name, int cost, int atk, int hp, int tier, String trait) {
        this.slot = slot;
        this.name = name;
        this.cost = cost;
        this.atk = atk;
        this.hp = hp;
        this.tier = tier;
        this.trait = trait == null ? "" : trait;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public int getAtk() {
        return atk;
    }

    public void setAtk(int atk) {
        this.atk = atk;
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = hp;
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
}
