package com.carview.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carview.common.model.AlarmEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AlarmMapper extends BaseMapper<AlarmEvent> {
}
