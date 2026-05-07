package com.closetruth.task;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final WalletService walletService;

    public TaskService(TaskRepository taskRepository, WalletService walletService) {
        this.taskRepository = taskRepository;
        this.walletService = walletService;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listTasks() {
        return taskRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional
    public TaskResponse createTask(String title) {
        String normalized = title == null ? "" : title.trim();
        if (normalized.isEmpty()) {
            normalized = "新任务";
        }
        int bonus = randomCreateBonus();
        TaskEntity task = new TaskEntity(normalized, bonus, randomCreateMessage());
        
        // Per requirement: random initial accumulated rewards
        InitialReward init = rollInitialAccumulated(bonus);
        if (init.gold() > 0 || init.diamonds() > 0) {
            task.addAccumulated(init.gold(), init.diamonds());
        }
        
        return TaskResponse.from(taskRepository.save(task));
    }

    private InitialReward rollInitialAccumulated(int bonus) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // Give some initial gold/diamonds based on the bonus points
        // ~30% chance of initial reward
        if (rnd.nextInt(100) < 70) {
            return new InitialReward(0, 0);
        }
        int gold = rnd.nextInt(1, (bonus / 2) + 2);
        int diamonds = rnd.nextInt(100) < 5 ? 1 : 0;
        return new InitialReward(gold, diamonds);
    }

    private record InitialReward(int gold, int diamonds) {}

    @Transactional
    public TaskResponse startTask(Long id) {
        TaskEntity task = getTask(id);
        task.start();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse pauseTask(Long id) {
        TaskEntity task = getTask(id);
        task.pause();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse resumeTask(Long id) {
        TaskEntity task = getTask(id);
        task.start();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse finishTask(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.FINISHED) {
            return TaskResponse.from(task);
        }

        long totalPoints = task.getTotalPoints();
        FinishRoll roll = rollFinishRewards(totalPoints);

        // include accumulated client-side ticks into final awarded amounts
        int clientGold = task.getAccumulatedGold();
        int clientDiamonds = task.getAccumulatedDiamonds();

        int totalGold = roll.gold() + clientGold;
        int totalDiamonds = roll.diamonds() + clientDiamonds;

        // apply finish rewards (server roll only used for summary and server-awarded portion)
        task.applyFinishRewards(roll.summary(), totalGold, totalDiamonds);

        // credit wallet with both server roll and client accumulated
        walletService.add(totalGold, totalDiamonds);

        // clear accumulated - already cleared in applyFinishRewards
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse accumulateTick(Long id, int gold, int diamonds) {
        if (gold <= 0 && diamonds <= 0) {
            return TaskResponse.from(getTask(id));
        }
        TaskEntity task = getTask(id);
        // only allow accumulation when task is running
        if (task.getStatus() != TaskStatus.RUNNING) {
            return TaskResponse.from(task);
        }
        task.addAccumulated(gold, diamonds);
        return TaskResponse.from(taskRepository.save(task));
    }

    private TaskEntity getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private int randomCreateBonus() {
        return 1 + ThreadLocalRandom.current().nextInt(18);
    }

    private String randomCreateMessage() {
        String[] rewards = {
                "小宝箱",
                "幸运星",
                "专注加成",
                "今日好运",
                "起步冲刺",
                "灵感火花"
        };
        return rewards[ThreadLocalRandom.current().nextInt(rewards.length)];
    }

    private FinishRoll rollFinishRewards(long totalPoints) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        long base = Math.max(1, totalPoints);
        if (rnd.nextInt(100) < 42) {
            return new FinishRoll(
                    "本次结算：运气一般，没有获得金币或钻石（累计积分 " + totalPoints + ")",
                    0,
                    0
            );
        }

        int upper = (int) Math.min(200L, Math.max(3L, base * 3L));
        int gold = rnd.nextInt(1, upper + 1);
        int diamonds = rnd.nextInt(100) < 18 ? 1 : 0;

        StringBuilder summary = new StringBuilder("本次结算：获得金币 ");
        summary.append(gold);
        if (diamonds > 0) {
            summary.append("，钻石 ").append(diamonds);
        }
        summary.append("（累计积分 ").append(totalPoints).append(")");
        return new FinishRoll(summary.toString(), gold, diamonds);
    }

    private record FinishRoll(String summary, int gold, int diamonds) {
    }
}
