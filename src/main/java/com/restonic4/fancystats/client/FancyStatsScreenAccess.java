package com.restonic4.fancystats.client;

import com.restonic4.fancystats.core.StatsReport;

public interface FancyStatsScreenAccess {
    void fancystats$receiveReport(StatsReport report);
}
