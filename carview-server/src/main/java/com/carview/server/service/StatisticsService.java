package com.carview.server.service;

import com.carview.common.model.RealtimeAgg;
import com.carview.server.mapper.StatisticsMapper;
import org.springframework.stereotype.Service;

@Service
public class StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsService(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    public RealtimeAgg getLatestRealtime() {
        return statisticsMapper.getLatestAgg();
    }
}
