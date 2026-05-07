package com.closetruth.task;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WalletInitializer implements ApplicationRunner {

    private final WalletRepository walletRepository;

    public WalletInitializer(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (walletRepository.existsById(WalletEntity.SINGLETON_ID)) {
            return;
        }
        WalletEntity wallet = new WalletEntity();
        wallet.setId(WalletEntity.SINGLETON_ID);
        wallet.setGold(0);
        wallet.setDiamonds(0);
        walletRepository.save(wallet);
    }
}
