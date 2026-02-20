package com.carview.server.service;

import com.carview.server.mapper.TrackMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TrackService {

    private final TrackMapper trackMapper;

    public TrackService(TrackMapper trackMapper) {
        this.trackMapper = trackMapper;
    }

    public List<Map<String, Object>> queryTrack(String vehicleId, String startTime, String endTime) {
        return trackMapper.queryTrack(vehicleId, startTime, endTime);
    }
}
