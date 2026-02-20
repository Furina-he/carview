package com.carview.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carview.common.model.AlarmEvent;
import com.carview.server.service.AlarmService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     @RequestParam(value = "level", required = false) Integer level,
                                     @RequestParam(value = "status", required = false) Integer status) {
        IPage<AlarmEvent> result = alarmService.list(page, size, level, status);
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @PutMapping("/{id}/ack")
    public Map<String, Object> acknowledge(@PathVariable("id") Long id) {
        alarmService.acknowledge(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "告警已确认");
        return response;
    }
}
