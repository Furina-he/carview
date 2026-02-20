package com.carview.server.controller;

import com.carview.server.service.TrackService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/{vehicleId}/track")
    public List<Map<String, Object>> queryTrack(@PathVariable("vehicleId") String vehicleId,
                                                 @RequestParam("startTime") String startTime,
                                                 @RequestParam("endTime") String endTime) {
        return trackService.queryTrack(vehicleId, startTime, endTime);
    }
}
