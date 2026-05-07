package com.closetruth.autochess;

import com.closetruth.autochess.dto.AutochessGameResponse;
import com.closetruth.autochess.dto.AutochessPayload;
import com.closetruth.autochess.dto.BoardIndexRequest;
import com.closetruth.autochess.dto.FightUnit;
import com.closetruth.autochess.dto.PlaceRequest;
import com.closetruth.autochess.dto.ShopOffer;
import com.closetruth.autochess.dto.SlotRequest;
import com.closetruth.autochess.dto.UnitIdRequest;
import com.closetruth.autochess.persist.AutochessSaveEntity;
import com.closetruth.autochess.persist.AutochessSaveRepository;
import com.closetruth.task.WalletResponse;
import com.closetruth.task.WalletService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AutochessGameService {

    private static final int BOARD_SLOTS = 4;
    private static final int SHOP_SLOTS = 5;
    private static final int BENCH_MAX = 8;
    private static final int REFRESH_COST_GOLD = 3;
    private static final int REVIVE_COST_DIAMONDS = 5;

    private static final String[] ENEMY_NAMES = {
            "游荡刺猬团", "遗忘石像", "拖延巨口", "番茄巢穴守卫", "锈铁巡逻队",
            "雾狐群落", "时间裂隙兽", "纸龙幼雏", "盐晶傀儡", "橡果精集群"
    };

    private record Template(String name, int cost, int atk, int hp, int tier, String trait) {
    }

    private static final List<Template> TEMPLATES = List.of(
            new Template("番茄民兵", 2, 2, 5, 1, "番茄"),
            new Template("专注游侠", 3, 3, 4, 1, "敏捷"),
            new Template("拖延史莱姆", 2, 1, 7, 1, "拖延"),
            new Template("橡果术士", 4, 4, 4, 2, "自然"),
            new Template("锈铁炮手", 4, 5, 3, 2, "重装"),
            new Template("雾狐刺客", 5, 6, 3, 2, "敏捷"),
            new Template("盐晶守卫", 5, 3, 8, 2, "重装"),
            new Template("时间窃贼", 6, 7, 5, 3, "时间"),
            new Template("番茄钟祭司", 6, 4, 9, 3, "番茄"),
            new Template("纸龙", 7, 8, 6, 3, "自然")
    );

    private static final Map<String, String> TRAIT_FALLBACK = Map.ofEntries(
            Map.entry("番茄民兵", "番茄"),
            Map.entry("番茄钟祭司", "番茄"),
            Map.entry("专注游侠", "敏捷"),
            Map.entry("雾狐刺客", "敏捷"),
            Map.entry("拖延史莱姆", "拖延"),
            Map.entry("橡果术士", "自然"),
            Map.entry("纸龙", "自然"),
            Map.entry("锈铁炮手", "重装"),
            Map.entry("盐晶守卫", "重装"),
            Map.entry("时间窃贼", "时间")
    );

    private record SynergyInfo(int bonus, String summary) {
    }

    private final AutochessSaveRepository saveRepository;
    private final WalletService walletService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AutochessGameService(AutochessSaveRepository saveRepository, WalletService walletService) {
        this.saveRepository = saveRepository;
        this.walletService = walletService;
    }

    @Transactional
    public AutochessGameResponse getGame() {
        AutochessPayload payload = loadOrCreate();
        normalize(payload);
        return respond(payload, "");
    }

    @Transactional
    public AutochessGameResponse refreshShop() {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        walletService.spend(REFRESH_COST_GOLD, 0);
        p.setShop(generateShop(p.getRound()));
        p.setLastLog("商店已刷新（-" + REFRESH_COST_GOLD + " 金币），商品随回合略有高费概率。");
        persist(p);
        return respond(p, "商店已刷新");
    }

    @Transactional
    public AutochessGameResponse buy(SlotRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        int slot = req.slot();
        if (slot < 0 || slot >= SHOP_SLOTS) {
            throw new IllegalStateException("INVALID_SLOT");
        }
        List<ShopOffer> shop = p.getShop();
        if (slot >= shop.size() || shop.get(slot) == null) {
            throw new IllegalStateException("EMPTY_SLOT");
        }
        if (p.getBench().size() >= BENCH_MAX) {
            throw new IllegalStateException("BENCH_FULL");
        }
        ShopOffer offer = shop.get(slot);
        walletService.spend(offer.getCost(), 0);
        FightUnit unit = FightUnit.fromShop(offer, UUID.randomUUID().toString());
        p.getBench().add(unit);
        shop.set(slot, null);
        p.setLastLog("购入「" + unit.getName() + "」（-" + offer.getCost() + " 金）·" + unit.getTrait() + " 羁绊，已入备战席。");
        persist(p);
        return respond(p, "购买成功");
    }

    @Transactional
    public AutochessGameResponse merge(UnitIdRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        FightUnit a = findOnBench(p, req.unitId());
        if (a == null) {
            throw new IllegalStateException("UNIT_NOT_ON_BENCH");
        }
        if (a.getStars() >= 3) {
            throw new IllegalStateException("STARS_MAX");
        }
        FightUnit b = findMergePartner(p, a);
        if (b == null) {
            throw new IllegalStateException("NO_MERGE_PAIR");
        }
        String idA = a.getId();
        String idB = b.getId();
        p.getBench().removeIf(u -> idA.equals(u.getId()) || idB.equals(u.getId()));
        FightUnit merged = mergeInto(a, b);
        p.getBench().add(merged);
        p.setLastLog("合成成功：「" + merged.getName() + "」升至 ★" + merged.getStars() + "（攻" + merged.getAtk() + " 血" + merged.getMaxHp() + "）。");
        persist(p);
        return respond(p, "合成成功");
    }

    @Transactional
    public AutochessGameResponse place(PlaceRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        int bi = req.boardIndex();
        if (bi < 0 || bi >= BOARD_SLOTS) {
            throw new IllegalStateException("INVALID_SLOT");
        }
        FightUnit onBoard = p.getBoard().get(bi);
        if (onBoard != null) {
            throw new IllegalStateException("BOARD_OCCUPIED");
        }
        FightUnit unit = removeFromBench(p, req.unitId());
        if (unit == null) {
            throw new IllegalStateException("UNIT_NOT_FOUND");
        }
        p.getBoard().set(bi, unit);
        p.setLastLog("「" + unit.getName() + "」★" + unit.getStars() + " 已上阵 #" + (bi + 1) + "。");
        persist(p);
        return respond(p, "上阵成功");
    }

    @Transactional
    public AutochessGameResponse unplace(BoardIndexRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        int bi = req.boardIndex();
        if (bi < 0 || bi >= BOARD_SLOTS) {
            throw new IllegalStateException("INVALID_SLOT");
        }
        FightUnit unit = p.getBoard().get(bi);
        if (unit == null) {
            throw new IllegalStateException("EMPTY_SLOT");
        }
        if (p.getBench().size() >= BENCH_MAX) {
            throw new IllegalStateException("BENCH_FULL");
        }
        p.getBoard().set(bi, null);
        p.getBench().add(unit);
        p.setLastLog("「" + unit.getName() + "」撤回备战席。");
        persist(p);
        return respond(p, "下阵成功");
    }

    @Transactional
    public AutochessGameResponse sell(UnitIdRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        FightUnit unit = removeFromBench(p, req.unitId());
        if (unit == null) {
            unit = removeFromBoard(p, req.unitId());
        }
        if (unit == null) {
            throw new IllegalStateException("UNIT_NOT_FOUND");
        }
        int price = 2 + unit.getTier() * 2 + unit.getStars() * 3;
        walletService.add(price, 0);
        p.setLastLog("出售「" + unit.getName() + "」★" + unit.getStars() + "，+" + price + " 金。");
        persist(p);
        return respond(p, "已出售");
    }

    @Transactional
    public AutochessGameResponse upgrade(UnitIdRequest req) {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        FightUnit unit = findUnit(p, req.unitId());
        if (unit == null) {
            throw new IllegalStateException("UNIT_NOT_FOUND");
        }
        walletService.spend(0, 1);
        unit.setAtk(unit.getAtk() + 2);
        unit.setMaxHp(unit.getMaxHp() + 2);
        unit.setCurrentHp(Math.min(unit.getMaxHp(), unit.getCurrentHp() + 2));
        p.setLastLog("钻石强化「" + unit.getName() + "」：攻+2、生命上限+2（-1 钻）。");
        persist(p);
        return respond(p, "强化成功");
    }

    @Transactional
    public AutochessGameResponse fight() {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        assertCanAct(p);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int r = p.getRound();
        refreshDerivedState(p);
        String foeName = p.getNextEnemyName() != null ? p.getNextEnemyName() : ENEMY_NAMES[0];
        int enemy = 8 + r * 6 + rnd.nextInt(10);
        SynergyInfo syn = synergyInfo(p);
        int basePlayer = 0;
        for (FightUnit u : p.getBoard()) {
            if (u != null) {
                basePlayer += u.combatPower();
            }
        }
        int player = basePlayer + syn.bonus();
        int playerRoll = player + rnd.nextInt(12);
        int enemyRoll = enemy + rnd.nextInt(12);
        StringBuilder log = new StringBuilder();
        log.append("第 ").append(r).append(" 轮 vs ").append(foeName)
                .append("：基础战力 ").append(basePlayer);
        if (syn.bonus() > 0) {
            log.append(" + 羁绊 ").append(syn.bonus());
        }
        log.append(" = ").append(player)
                .append("（掷骰 ").append(playerRoll).append("） vs 敌 ").append(enemy)
                .append("（掷骰 ").append(enemyRoll).append("）。");

        if (playerRoll >= enemyRoll) {
            p.setWinStreak(p.getWinStreak() + 1);
            p.setLoseStreak(0);
            int streakBonus = Math.min(4, Math.max(0, p.getWinStreak() - 1));
            int goldReward = 5 + r + streakBonus;
            walletService.add(goldReward, 0);
            int diamondBonus = rnd.nextInt(100) < 12 ? 1 : 0;
            if (diamondBonus > 0) {
                walletService.add(0, diamondBonus);
            }
            log.append(" 胜利！+").append(goldReward).append(" 金");
            if (streakBonus > 0) {
                log.append("（含连胜 ").append(streakBonus).append("）");
            }
            if (diamondBonus > 0) {
                log.append("，+").append(diamondBonus).append(" 钻");
            }
            log.append("。连胜 ").append(p.getWinStreak()).append("。");
        } else {
            p.setLoseStreak(p.getLoseStreak() + 1);
            p.setWinStreak(0);
            int dmg = 8 + r / 2 + rnd.nextInt(6);
            if (p.getLoseStreak() >= 2) {
                dmg += 2;
            }
            p.setPlayerHp(p.getPlayerHp() - dmg);
            log.append(" 败北，-").append(dmg).append(" 生命（连败 ").append(p.getLoseStreak()).append(" 额外压力）。");
            log.append(" 剩余 ").append(Math.max(0, p.getPlayerHp())).append("。");
            if (p.getPlayerHp() <= 0) {
                p.setPlayerHp(0);
                p.setGameOver(true);
                log.append(" 游戏结束：可 ").append(REVIVE_COST_DIAMONDS).append(" 钻复活或重开。");
            }
        }
        p.setRound(r + 1);
        healBoard(p);
        p.setLastLog(log.toString());
        persist(p);
        return respond(p, "战斗结算完成");
    }

    @Transactional
    public AutochessGameResponse resetRun() {
        AutochessPayload p = AutochessPayload.newRun();
        p.setShop(generateShop(p.getRound()));
        refreshDerivedState(p);
        persist(p);
        return respond(p, "已重开新局");
    }

    @Transactional
    public AutochessGameResponse revive() {
        AutochessPayload p = loadOrCreate();
        normalize(p);
        if (!p.isGameOver()) {
            throw new IllegalStateException("NEED_GAME_OVER");
        }
        walletService.spend(0, REVIVE_COST_DIAMONDS);
        p.setGameOver(false);
        p.setPlayerHp(60);
        p.setLoseStreak(0);
        p.setLastLog("复活：生命 60（-" + REVIVE_COST_DIAMONDS + " 钻），连败清零。");
        persist(p);
        return respond(p, "已复活");
    }

    private static void assertCanAct(AutochessPayload p) {
        if (p.isGameOver()) {
            throw new IllegalStateException("GAME_OVER");
        }
    }

    private static void healBoard(AutochessPayload p) {
        for (FightUnit u : p.getBoard()) {
            if (u != null) {
                u.fullHeal();
            }
        }
    }

    private static FightUnit findOnBench(AutochessPayload p, String unitId) {
        for (FightUnit u : p.getBench()) {
            if (u.getId().equals(unitId)) {
                return u;
            }
        }
        return null;
    }

    private static FightUnit findMergePartner(AutochessPayload p, FightUnit a) {
        for (FightUnit u : p.getBench()) {
            if (u.getId().equals(a.getId())) {
                continue;
            }
            if (u.getName().equals(a.getName()) && u.getStars() == a.getStars() && u.getStars() < 3) {
                return u;
            }
        }
        return null;
    }

    private static FightUnit mergeInto(FightUnit a, FightUnit b) {
        FightUnit m = new FightUnit();
        m.setId(UUID.randomUUID().toString());
        m.setName(a.getName());
        m.setTrait(!a.getTrait().isEmpty() ? a.getTrait() : b.getTrait());
        int tier = Math.max(a.getTier(), b.getTier());
        m.setTier(tier);
        int ns = a.getStars() + 1;
        m.setStars(Math.min(3, ns));
        m.setAtk((a.getAtk() + b.getAtk()) / 2 + 3 + tier);
        m.setMaxHp((a.getMaxHp() + b.getMaxHp()) / 2 + 5 + tier * 2);
        m.setCurrentHp(m.getMaxHp());
        return m;
    }

    private static FightUnit removeFromBench(AutochessPayload p, String unitId) {
        List<FightUnit> bench = p.getBench();
        for (int i = 0; i < bench.size(); i++) {
            FightUnit u = bench.get(i);
            if (u.getId().equals(unitId)) {
                bench.remove(i);
                return u;
            }
        }
        return null;
    }

    private static FightUnit removeFromBoard(AutochessPayload p, String unitId) {
        List<FightUnit> board = p.getBoard();
        for (int i = 0; i < board.size(); i++) {
            FightUnit u = board.get(i);
            if (u != null && u.getId().equals(unitId)) {
                board.set(i, null);
                return u;
            }
        }
        return null;
    }

    private static FightUnit findUnit(AutochessPayload p, String unitId) {
        FightUnit u = findOnBench(p, unitId);
        if (u != null) {
            return u;
        }
        for (FightUnit x : p.getBoard()) {
            if (x != null && x.getId().equals(unitId)) {
                return x;
            }
        }
        return null;
    }

    private AutochessPayload loadOrCreate() {
        return saveRepository.findById(AutochessSaveEntity.SINGLETON_ID)
                .map(entity -> readPayload(entity.getPayload()))
                .orElseGet(() -> {
                    AutochessPayload fresh = AutochessPayload.newRun();
                    fresh.setShop(generateShop(fresh.getRound()));
                    persist(fresh);
                    return fresh;
                });
    }

    private AutochessPayload readPayload(String json) {
        try {
            return objectMapper.readValue(json, AutochessPayload.class);
        } catch (JsonProcessingException e) {
            AutochessPayload fresh = AutochessPayload.newRun();
            fresh.setShop(generateShop(fresh.getRound()));
            return fresh;
        }
    }

    private void persist(AutochessPayload payload) {
        try {
            refreshDerivedState(payload);
            String json = objectMapper.writeValueAsString(payload);
            saveRepository.findById(AutochessSaveEntity.SINGLETON_ID)
                    .ifPresentOrElse(
                            entity -> {
                                entity.setPayload(json);
                                saveRepository.save(entity);
                            },
                            () -> saveRepository.save(new AutochessSaveEntity(json))
                    );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("SAVE_FAILED");
        }
    }

    private void normalize(AutochessPayload p) {
        if (p.getBench() == null) {
            p.setBench(new ArrayList<>());
        }
        if (p.getBoard() == null) {
            p.setBoard(new ArrayList<>());
        }
        while (p.getBoard().size() < BOARD_SLOTS) {
            p.getBoard().add(null);
        }
        while (p.getBoard().size() > BOARD_SLOTS) {
            p.getBoard().removeLast();
        }
        if (p.getShop() == null) {
            p.setShop(new ArrayList<>());
        }
        migrateLegacyOffers(p.getShop());
        migrateLegacyUnits(p.getBench());
        migrateLegacyUnitsBoard(p.getBoard());
        if (p.getShop().isEmpty()) {
            p.setShop(generateShop(p.getRound()));
        }
        while (p.getShop().size() < SHOP_SLOTS) {
            p.getShop().add(null);
        }
        while (p.getShop().size() > SHOP_SLOTS) {
            p.getShop().removeLast();
        }
        refreshDerivedState(p);
    }

    private static void migrateLegacyOffers(List<ShopOffer> shop) {
        for (ShopOffer o : shop) {
            if (o == null) {
                continue;
            }
            if (o.getTrait() == null || o.getTrait().isEmpty()) {
                o.setTrait(TRAIT_FALLBACK.getOrDefault(o.getName(), "中立"));
            }
        }
    }

    private static void migrateLegacyUnits(List<FightUnit> units) {
        for (FightUnit u : units) {
            patchUnit(u);
        }
    }

    private static void migrateLegacyUnitsBoard(List<FightUnit> board) {
        for (FightUnit u : board) {
            if (u != null) {
                patchUnit(u);
            }
        }
    }

    private static void patchUnit(FightUnit u) {
        if (u.getTrait() == null || u.getTrait().isEmpty()) {
            u.setTrait(TRAIT_FALLBACK.getOrDefault(u.getName(), "中立"));
        }
        if (u.getStars() <= 0) {
            u.setStars(1);
        }
    }

    private void refreshDerivedState(AutochessPayload p) {
        int r = Math.max(1, p.getRound());
        p.setNextEnemyName(ENEMY_NAMES[(r - 1) % ENEMY_NAMES.length]);
        p.setNextEnemyPowerBase(8 + r * 6);
        SynergyInfo s = synergyInfo(p);
        p.setSynergySummary(s.summary());
    }

    private static SynergyInfo synergyInfo(AutochessPayload p) {
        Map<String, Integer> cnt = new HashMap<>();
        for (FightUnit u : p.getBoard()) {
            if (u == null) {
                continue;
            }
            String tr = u.getTrait();
            if (tr == null || tr.isEmpty() || "中立".equals(tr)) {
                continue;
            }
            cnt.merge(tr, 1, Integer::sum);
        }
        int bonus = 0;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            int c = e.getValue();
            if (c < 2) {
                continue;
            }
            int b = 6 + (c - 2) * 5;
            bonus += b;
            if (!sb.isEmpty()) {
                sb.append("；");
            }
            sb.append(e.getKey()).append("×").append(c).append(" → +").append(b);
        }
        String summary = sb.isEmpty() ? "羁绊：暂无（棋盘需≥2枚同羁绊）" : "羁绊：" + sb;
        return new SynergyInfo(bonus, summary);
    }

    private static List<ShopOffer> generateShop(int round) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        List<ShopOffer> shop = new ArrayList<>();
        for (int slot = 0; slot < SHOP_SLOTS; slot++) {
            Template t = pickTemplate(round, rnd);
            shop.add(new ShopOffer(slot, t.name(), t.cost(), t.atk(), t.hp(), t.tier(), t.trait()));
        }
        return shop;
    }

    private static Template pickTemplate(int round, ThreadLocalRandom rnd) {
        List<Template> weighted = new ArrayList<>();
        for (Template t : TEMPLATES) {
            int w = switch (t.tier()) {
                case 1 -> Math.max(6, 42 - round * 2);
                case 2 -> 18 + Math.min(22, round * 2);
                case 3 -> Math.min(28, 5 + round * 2);
                default -> 10;
            };
            if (round < 3 && t.tier() == 3) {
                w = Math.max(2, w - 12);
            }
            for (int i = 0; i < w; i++) {
                weighted.add(t);
            }
        }
        return weighted.get(rnd.nextInt(weighted.size()));
    }

    private AutochessGameResponse respond(AutochessPayload game, String message) {
        refreshDerivedState(game);
        WalletResponse wallet = walletService.getWallet();
        return new AutochessGameResponse(game, wallet, message);
    }
}
