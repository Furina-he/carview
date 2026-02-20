package com.carview.server.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.carview.common.model.AlarmEvent;
import com.carview.server.mapper.AlarmMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AlarmService {

    private final AlarmMapper alarmMapper;

    public AlarmService(AlarmMapper alarmMapper) {
        this.alarmMapper = alarmMapper;
    }

    public IPage<AlarmEvent> list(int page, int size, Integer level, Integer status) {
        LambdaQueryWrapper<AlarmEvent> wrapper = new LambdaQueryWrapper<>();
        if (level != null) {
            wrapper.eq(AlarmEvent::getAlarmLevel, level);
        }
        if (status != null) {
            wrapper.eq(AlarmEvent::getAlarmStatus, status);
        }
        wrapper.orderByDesc(AlarmEvent::getEventTime);
        return alarmMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void acknowledge(Long id) {
        alarmMapper.update(null, new LambdaUpdateWrapper<AlarmEvent>()
                .eq(AlarmEvent::getId, id)
                .set(AlarmEvent::getAlarmStatus, 2)
                .set(AlarmEvent::getAckTime, LocalDateTime.now()));
    }
}
