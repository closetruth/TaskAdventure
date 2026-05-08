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
        TaskEntity task = new TaskEntity(normalized);
        return TaskResponse.from(taskRepository.save(task));
    }

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
    public TaskResponse setPlannedTime(Long id, int minutes) {
        if (minutes < 5) {
            throw new IllegalArgumentException("计划时间最少为5分钟");
        }
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.FINISHED) {
            throw new IllegalArgumentException("已完成的任务无法设置计划时间");
        }
        task.setPlannedMinutes(minutes);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse checkAndAutoPause(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.RUNNING && task.shouldAutoPause()) {
            task.pause();
            taskRepository.save(task);
        }
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse finishTask(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.FINISHED) {
            return TaskResponse.from(task);
        }

        // Check if planned time is set and not yet reached
        Integer plannedMinutes = task.getPlannedMinutes();
        if (plannedMinutes != null && plannedMinutes > 0) {
            long elapsedMinutes = task.getCurrentElapsedSeconds() / 60;
            if (elapsedMinutes < plannedMinutes) {
                long remainingMinutes = plannedMinutes - elapsedMinutes;
                throw new IllegalArgumentException(
                    String.format("还需专注 %d 分钟才能完成任务（已专注 %d 分钟，计划 %d 分钟）",
                        remainingMinutes, elapsedMinutes, plannedMinutes)
                );
            }
        }

        // Calculate completion bonus based on elapsed time (more time = more bonus)
        CompletionBonus bonus = calculateCompletionBonus(task);

        // include accumulated client-side ticks into final awarded amounts
        int clientGold = task.getAccumulatedGold();
        int clientDiamonds = task.getAccumulatedDiamonds();

        // Total rewards: pending + accumulated + completion bonus
        int totalGold = task.getPendingGold() + clientGold + bonus.gold();
        int totalDiamonds = task.getPendingDiamonds() + clientDiamonds + bonus.diamonds();

        // apply finish rewards
        task.applyFinishRewards(totalGold, totalDiamonds);

        // credit wallet with all rewards
        walletService.add(totalGold, totalDiamonds);

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

    @Transactional
    public TaskResponse addPendingReward(Long id, int gold, int diamonds) {
        if (gold <= 0 && diamonds <= 0) {
            return TaskResponse.from(getTask(id));
        }
        TaskEntity task = getTask(id);
        if (task.getStatus() != TaskStatus.RUNNING) {
            return TaskResponse.from(task);
        }
        task.addPendingRewards(gold, diamonds);
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse claimRewards(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.FINISHED) {
            throw new IllegalArgumentException("已完成的任务无法领取奖励");
        }
        int gold = task.getPendingGold();
        int diamonds = task.getPendingDiamonds();
        if (gold > 0 || diamonds > 0) {
            walletService.add(gold, diamonds);
            task.claimPendingRewards();
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new IllegalArgumentException("运行中的任务无法删除，请先暂停");
        }
        taskRepository.delete(task);
    }

    private TaskEntity getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
    }

    private CompletionBonus calculateCompletionBonus(TaskEntity task) {
        long elapsedMinutes = task.getCurrentElapsedSeconds() / 60;
        Integer plannedMinutes = task.getPlannedMinutes();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        
        // Base bonus scales with time spent
        int baseGold = (int) (elapsedMinutes * 2); // 2 gold per minute
        int baseDiamonds = (int) (elapsedMinutes / 10); // 1 diamond per 10 minutes
        
        // If planned time is set and actual time >= planned time, give bonus multiplier
        if (plannedMinutes != null && plannedMinutes > 0 && elapsedMinutes >= plannedMinutes) {
            // Bonus for completing planned time: 50% extra
            double multiplier = 1.5;
            baseGold = (int) (baseGold * multiplier);
            baseDiamonds = (int) (baseDiamonds * multiplier);
        }
        
        // Add some randomness (±20%)
        int variance = (int) (baseGold * 0.2);
        int finalGold = Math.max(10, baseGold + rnd.nextInt(-variance, variance + 1));
        int finalDiamonds = baseDiamonds + (rnd.nextInt(100) < 20 ? 1 : 0); // 20% chance for extra diamond
        
        return new CompletionBonus(finalGold, finalDiamonds);
    }

    private FinishRoll rollFinishRewards() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        // Base random rewards since points are gone
        if (rnd.nextInt(100) < 42) {
            return new FinishRoll(0, 0);
        }

        int gold = rnd.nextInt(5, 51); // 5..50 gold
        int diamonds = rnd.nextInt(100) < 10 ? 1 : 0; // 10% chance for 1 diamond

        return new FinishRoll(gold, diamonds);
    }

    private record FinishRoll(int gold, int diamonds) {
    }

    private record CompletionBonus(int gold, int diamonds) {
    }
}
