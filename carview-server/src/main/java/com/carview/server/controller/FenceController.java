package com.carview.server.controller;

import com.carview.common.model.FenceRule;
import com.carview.server.service.FenceService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fences")
public class FenceController {

    private final FenceService fenceService;

    public FenceController(FenceService fenceService) {
        this.fenceService = fenceService;
    }

    @GetMapping
    public List<FenceRule> list() {
        return fenceService.listAll();
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody FenceRule fence) {
        fenceService.create(fence);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "围栏创建成功");
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable("id") Long id, @RequestBody FenceRule fence) {
        fence.setId(id);
        fenceService.update(fence);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "围栏更新成功");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") Long id) {
        fenceService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "围栏删除成功");
        return response;
    }
}
