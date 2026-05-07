package com.closetruth.game;

import com.closetruth.autochess.AutochessController;
import com.closetruth.season.SeasonController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {AutochessController.class, SeasonController.class})
public class GameExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> illegalState(IllegalStateException ex) {
        String code = ex.getMessage();
        String message = switch (code) {
            case "INSUFFICIENT_FUNDS" -> "余额不足";
            case "ALREADY_CLAIMED" -> "本周已领取过结算奖励";
            case "GAME_OVER" -> "本局已结束：请复活或重开";
            case "NEED_GAME_OVER" -> "仅在游戏结束时可复活";
            case "INVALID_SLOT" -> "位置不合法";
            case "EMPTY_SLOT" -> "该格为空";
            case "BENCH_FULL" -> "备战席已满（最多 8）";
            case "BOARD_OCCUPIED" -> "棋盘该格已有棋子";
            case "UNIT_NOT_FOUND" -> "找不到该棋子";
            case "UNIT_NOT_ON_BENCH" -> "仅能在备战席合成：请先将棋子下阵";
            case "NO_MERGE_PAIR" -> "需要备战席上另一枚同名且同星级的棋子";
            case "STARS_MAX" -> "该棋子已是 ★★★，无法继续合成";
            case "SAVE_FAILED" -> "存档写入失败";
            default -> code != null && !code.isBlank() ? code : "请求无法完成";
        };
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }
}
