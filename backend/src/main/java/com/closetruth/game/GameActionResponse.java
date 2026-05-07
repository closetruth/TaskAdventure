package com.closetruth.game;

import com.closetruth.task.WalletResponse;

public record GameActionResponse(String message, WalletResponse wallet, String detail) {
}
