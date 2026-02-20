package com.carview.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carview.common.model.VehicleInfo;
import com.carview.server.mapper.VehicleMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VehicleService {

    private final VehicleMapper vehicleMapper;

    public VehicleService(VehicleMapper vehicleMapper) {
        this.vehicleMapper = vehicleMapper;
    }

    public IPage<VehicleInfo> list(int page, int size, String keyword) {
        LambdaQueryWrapper<VehicleInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(VehicleInfo::getVehicleId, keyword)
                    .or().like(VehicleInfo::getBrand, keyword)
                    .or().like(VehicleInfo::getOwnerName, keyword);
        }
        wrapper.orderByDesc(VehicleInfo::getCreatedAt);
        return vehicleMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public VehicleInfo getById(Long id) {
        return vehicleMapper.selectById(id);
    }

    public void create(VehicleInfo vehicle) {
        vehicleMapper.insert(vehicle);
    }

    public void update(VehicleInfo vehicle) {
        vehicleMapper.updateById(vehicle);
    }

    public void delete(Long id) {
        vehicleMapper.deleteById(id);
    }
}
