package com.closetruth.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet() {
        return WalletResponse.from(requireWallet());
    }

    @Transactional
    public WalletResponse add(long goldDelta, long diamondDelta) {
        WalletEntity wallet = requireWallet();
        wallet.addGold(goldDelta);
        wallet.addDiamonds(diamondDelta);
        return WalletResponse.from(walletRepository.save(wallet));
    }

    /**
     * 扣除余额；不足时抛出 IllegalStateException("INSUFFICIENT_FUNDS")。
     */
    @Transactional
    public WalletResponse spend(long goldCost, long diamondCost) {
        WalletEntity wallet = requireWallet();
        if (wallet.getGold() < goldCost || wallet.getDiamonds() < diamondCost) {
            throw new IllegalStateException("INSUFFICIENT_FUNDS");
        }
        wallet.addGold(-goldCost);
        wallet.addDiamonds(-diamondCost);
        return WalletResponse.from(walletRepository.save(wallet));
    }

    private WalletEntity requireWallet() {
        return walletRepository.findById(WalletEntity.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Wallet not initialized"));
    }
}
