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

    @PostMapping("/task/{id}/finish")
    public TaskResponse finishTask(@PathVariable Long id) {
        return taskService.finishTask(id);
    }

    // Client-side tick accumulation endpoint (gold/diamonds from 10s ticks)
    @PostMapping("/task/{id}/tick")
    public TaskResponse tick(@PathVariable Long id, @RequestParam(defaultValue = "0") int gold, @RequestParam(defaultValue = "0") int diamonds) {
        return taskService.accumulateTick(id, gold, diamonds);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
