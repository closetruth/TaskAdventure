package com.closetruth.season;

import com.closetruth.task.WalletResponse;

public record WeekSummaryResponse(
        String weekKey,
        String weekRangeLabel,
        String zoneId,
        long tasksCompletedThisWeek,
        long focusMinutesThisWeek,
        String rankTitle,
        int rankTier,
        int settlementGoldPreview,
        boolean claimedThisWeek,
        WalletResponse wallet
) {
}
