package com.closetruth.autochess.dto;

import com.closetruth.task.WalletResponse;

public record AutochessGameResponse(AutochessPayload game, WalletResponse wallet, String message) {
}
