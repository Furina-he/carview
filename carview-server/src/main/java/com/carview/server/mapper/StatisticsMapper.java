package com.carview.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carview.common.model.RealtimeAgg;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface StatisticsMapper extends BaseMapper<RealtimeAgg> {

    @Select("SELECT * FROM realtime_agg_10s ORDER BY window_end DESC LIMIT 1")
    RealtimeAgg getLatestAgg();
}
