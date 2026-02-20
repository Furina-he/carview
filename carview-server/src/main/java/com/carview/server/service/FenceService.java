package com.carview.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carview.common.model.FenceRule;
import com.carview.server.mapper.FenceMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FenceService {

    private final FenceMapper fenceMapper;

    public FenceService(FenceMapper fenceMapper) {
        this.fenceMapper = fenceMapper;
    }

    public List<FenceRule> listAll() {
        return fenceMapper.selectList(new LambdaQueryWrapper<FenceRule>()
                .orderByDesc(FenceRule::getCreatedAt));
    }

    public void create(FenceRule fence) {
        fenceMapper.insert(fence);
    }

    public void update(FenceRule fence) {
        fenceMapper.updateById(fence);
    }

    public void delete(Long id) {
        fenceMapper.deleteById(id);
    }
}
