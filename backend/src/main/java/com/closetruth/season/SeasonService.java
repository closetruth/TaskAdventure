package com.closetruth.season;

import com.closetruth.game.GameActionResponse;
import com.closetruth.task.TaskRepository;
import com.closetruth.task.TaskStatus;
import com.closetruth.task.WalletService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;

@Service
public class SeasonService {

    private final TaskRepository taskRepository;
    private final WeekClaimRepository weekClaimRepository;
    private final WalletService walletService;

    public SeasonService(
            TaskRepository taskRepository,
            WeekClaimRepository weekClaimRepository,
            WalletService walletService
    ) {
        this.taskRepository = taskRepository;
        this.weekClaimRepository = weekClaimRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public WeekSummaryResponse currentWeekSummary() {
        ZoneId zone = ZoneId.systemDefault();
        WeekBounds bounds = WeekBounds.current(zone);
        long seconds = taskRepository.sumElapsedSecondsFinishedBetween(
                TaskStatus.FINISHED,
                bounds.startInclusive(),
                bounds.endExclusive()
        );
        long tasks = taskRepository.countFinishedBetween(
                TaskStatus.FINISHED,
                bounds.startInclusive(),
                bounds.endExclusive()
        );
        long minutes = seconds / 60;
        Rank rank = rankFor(minutes, tasks);
        int preview = computeSettlementGold(tasks, minutes);
        boolean claimed = weekClaimRepository.existsById(bounds.weekKey());
        return new WeekSummaryResponse(
                bounds.weekKey(),
                bounds.rangeLabel(),
                zone.getId(),
                tasks,
                minutes,
                rank.title(),
                rank.tier(),
                preview,
                claimed,
                walletService.getWallet()
        );
    }

    @Transactional
    public GameActionResponse claimCurrentWeek() {
        ZoneId zone = ZoneId.systemDefault();
        WeekBounds bounds = WeekBounds.current(zone);
        if (weekClaimRepository.existsById(bounds.weekKey())) {
            throw new IllegalStateException("ALREADY_CLAIMED");
        }
        long seconds = taskRepository.sumElapsedSecondsFinishedBetween(
                TaskStatus.FINISHED,
                bounds.startInclusive(),
                bounds.endExclusive()
        );
        long tasks = taskRepository.countFinishedBetween(
                TaskStatus.FINISHED,
                bounds.startInclusive(),
                bounds.endExclusive()
        );
        long minutes = seconds / 60;
        int gold = computeSettlementGold(tasks, minutes);
        walletService.add(gold, 0);
        weekClaimRepository.save(new WeekClaimEntity(bounds.weekKey(), Instant.now(), gold));
        return new GameActionResponse(
                "本周排位结算已领取：金币 +" + gold,
                walletService.getWallet(),
                bounds.weekKey()
        );
    }

    private static int computeSettlementGold(long tasks, long focusMinutes) {
        long base = 15 + tasks * 5L + focusMinutes / 5L;
        return (int) Math.min(120L, Math.max(10L, base));
    }

    private record Rank(String title, int tier) {
    }

    private static Rank rankFor(long focusMinutes, long tasks) {
        long score = focusMinutes + tasks * 15L;
        if (score < 30) {
            return new Rank("见习旗手", 1);
        }
        if (score < 120) {
            return new Rank("坚韧棋手", 2);
        }
        if (score < 300) {
            return new Rank("专注导师", 3);
        }
        if (score < 600) {
            return new Rank("时空司令", 4);
        }
        return new Rank("传说造物主", 5);
    }
}
