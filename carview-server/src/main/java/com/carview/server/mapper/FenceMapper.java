package com.carview.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carview.common.model.FenceRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FenceMapper extends BaseMapper<FenceRule> {
}
