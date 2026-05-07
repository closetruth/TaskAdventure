package com.closetruth.task;

public record WalletResponse(long gold, long diamonds) {
    public static WalletResponse from(WalletEntity entity) {
        return new WalletResponse(entity.getGold(), entity.getDiamonds());
    }
}
