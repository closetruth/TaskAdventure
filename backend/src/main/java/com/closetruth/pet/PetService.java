package com.closetruth.pet;

import com.closetruth.task.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class PetService {

    private final PetRepository petRepository;
    private final WalletService walletService;

    // 宠物类型和价格
    private static final Map<String, Integer> PET_PRICES = Map.of(
            "cat", 50,
            "dog", 80,
            "rabbit", 60,
            "dragon", 200,
            "fox", 100,
            "panda", 150
    );

    private static final List<String> PET_TYPES = List.of("cat", "dog", "rabbit", "dragon", "fox", "panda");

    public PetService(PetRepository petRepository, WalletService walletService) {
        this.petRepository = petRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public List<PetResponse> listPets() {
        return petRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PetResponse::from)
                .toList();
    }

    @Transactional
    public PetResponse adoptPet(String name, String type) {
        if (!PET_PRICES.containsKey(type)) {
            throw new IllegalArgumentException("无效的宠物类型: " + type);
        }

        int price = PET_PRICES.get(type);
        var wallet = walletService.getWallet();

        if (wallet.gold() < price) {
            throw new IllegalArgumentException("金币不足！需要 " + price + " 金币");
        }

        // 扣除金币
        walletService.add(-price, 0);

        // 创建宠物
        PetEntity pet = new PetEntity(name, type);
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public PetResponse feedPet(Long id) {
        PetEntity pet = getPet(id);
        
        // 检查是否有足够的食物（可以用金币购买食物）
        var wallet = walletService.getWallet();
        if (wallet.gold() < 5) {
            throw new IllegalArgumentException("金币不足！喂食需要 5 金币");
        }

        walletService.add(-5, 0);
        pet.feed();
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public PetResponse playWithPet(Long id) {
        PetEntity pet = getPet(id);
        
        if (pet.getEnergy() < 20) {
            throw new IllegalArgumentException("宠物太累了，让它休息一下吧");
        }
        
        if (pet.getHunger() < 10) {
            throw new IllegalArgumentException("宠物太饿了，先喂喂它吧");
        }

        pet.play();
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public PetResponse trainPet(Long id) {
        PetEntity pet = getPet(id);
        
        if (pet.getEnergy() < 30) {
            throw new IllegalArgumentException("宠物太累了，无法训练");
        }
        
        if (pet.getHunger() < 20) {
            throw new IllegalArgumentException("宠物太饿了，先补充体力");
        }

        pet.train();
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public PetResponse restPet(Long id) {
        PetEntity pet = getPet(id);
        pet.rest();
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public void deletePet(Long id) {
        PetEntity pet = getPet(id);
        petRepository.delete(pet);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getShopInfo() {
        return Map.of(
                "pets", PET_PRICES,
                "types", PET_TYPES
        );
    }

    private PetEntity getPet(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("宠物不存在: " + id));
    }
}
