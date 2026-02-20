package com.carview.server.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.carview.common.model.VehicleInfo;
import com.carview.server.service.VehicleService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public Map<String, Object> list(@RequestParam(value = "page", defaultValue = "1") int page,
                                     @RequestParam(value = "size", defaultValue = "10") int size,
                                     @RequestParam(value = "keyword", required = false) String keyword) {
        IPage<VehicleInfo> result = vehicleService.list(page, size, keyword);
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", result.getCurrent());
        response.put("size", result.getSize());
        return response;
    }

    @GetMapping("/{id}")
    public VehicleInfo getById(@PathVariable("id") Long id) {
        return vehicleService.getById(id);
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody VehicleInfo vehicle) {
        vehicleService.create(vehicle);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "创建成功");
        return response;
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable("id") Long id, @RequestBody VehicleInfo vehicle) {
        vehicle.setId(id);
        vehicleService.update(vehicle);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "更新成功");
        return response;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable("id") Long id) {
        vehicleService.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "删除成功");
        return response;
    }
}
