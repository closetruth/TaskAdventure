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
    public TaskResponse finishTask(Long id) {
        TaskEntity task = getTask(id);
        if (task.getStatus() == TaskStatus.FINISHED) {
            return TaskResponse.from(task);
        }

        FinishRoll roll = rollFinishRewards();

        // include accumulated client-side ticks into final awarded amounts
        int clientGold = task.getAccumulatedGold();
        int clientDiamonds = task.getAccumulatedDiamonds();

        int totalGold = roll.gold() + clientGold;
        int totalDiamonds = roll.diamonds() + clientDiamonds;

        // apply finish rewards
        task.applyFinishRewards(totalGold, totalDiamonds);

        // credit wallet with both server roll and client accumulated
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

    private TaskEntity getTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
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
}
