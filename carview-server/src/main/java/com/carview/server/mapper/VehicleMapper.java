package com.carview.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carview.common.model.VehicleInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface VehicleMapper extends BaseMapper<VehicleInfo> {
}
