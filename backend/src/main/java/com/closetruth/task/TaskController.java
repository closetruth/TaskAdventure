package com.closetruth.task;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {
    private final TaskService taskService;
    private final WalletService walletService;

    public TaskController(TaskService taskService, WalletService walletService) {
        this.taskService = taskService;
        this.walletService = walletService;
    }

    @GetMapping("/wallet")
    public WalletResponse wallet() {
        return walletService.getWallet();
    }

    @GetMapping("/tasks")
    public List<TaskResponse> listTasks() {
        return taskService.listTasks();
    }

    @PostMapping("/task")
    public TaskResponse createTask(@RequestParam(defaultValue = "新任务") String title) {
        return taskService.createTask(title);
    }

    @PostMapping("/task/{id}/start")
    public TaskResponse startTask(@PathVariable Long id) {
        return taskService.startTask(id);
    }

    @PostMapping("/task/{id}/pause")
    public TaskResponse pauseTask(@PathVariable Long id) {
        return taskService.pauseTask(id);
    }

    @PostMapping("/task/{id}/resume")
    public TaskResponse resumeTask(@PathVariable Long id) {
        return taskService.resumeTask(id);
    }

    @PostMapping("/task/{id}/plan")
    public TaskResponse setPlannedTime(@PathVariable Long id, @RequestParam int minutes) {
        return taskService.setPlannedTime(id, minutes);
    }

    @PostMapping("/task/{id}/check-auto-pause")
    public TaskResponse checkAutoPause(@PathVariable Long id) {
        return taskService.checkAndAutoPause(id);
    }

    @PostMapping("/task/{id}/finish")
    public TaskResponse finishTask(@PathVariable Long id) {
        return taskService.finishTask(id);
    }

    // Client-side tick accumulation endpoint (gold/diamonds from 10s ticks)
    @PostMapping("/task/{id}/tick")
    public TaskResponse tick(@PathVariable Long id, @RequestParam(defaultValue = "0") int gold, @RequestParam(defaultValue = "0") int diamonds) {
        return taskService.accumulateTick(id, gold, diamonds);
    }

    // Add pending rewards for 10-minute cycle
    @PostMapping("/task/{id}/add-pending")
    public TaskResponse addPendingReward(@PathVariable Long id, @RequestParam(defaultValue = "0") int gold, @RequestParam(defaultValue = "0") int diamonds) {
        return taskService.addPendingReward(id, gold, diamonds);
    }

    // Claim pending rewards manually
    @PostMapping("/task/{id}/claim")
    public TaskResponse claimRewards(@PathVariable Long id) {
        return taskService.claimRewards(id);
    }

    // Delete task (only if not running)
    @PostMapping("/task/{id}/delete")
    public Map<String, String> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Map.of("message", "任务已删除");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
