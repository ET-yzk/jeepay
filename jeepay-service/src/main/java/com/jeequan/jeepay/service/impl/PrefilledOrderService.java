package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.entity.PrefilledOrder;
import com.jeequan.jeepay.service.mapper.PrefilledOrderMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>
 * 预填订单配置表 服务实现类
 * </p>
 *
 * @author [mybatis plus generator]
 * @since 2025-04-23
 */
@Service
public class PrefilledOrderService extends ServiceImpl<PrefilledOrderMapper, PrefilledOrder> {

    /**
     * 分页查询预填订单
     * @param iPage 分页参数
     * @param prefilledOrder 查询条件
     * @param paramJSON 其他查询参数 (如时间范围)
     * @param wrapper mybatis-plus 查询条件构造器
     * @return 分页结果
     */
    public IPage<PrefilledOrder> listByPage(IPage iPage, PrefilledOrder prefilledOrder, JSONObject paramJSON, LambdaQueryWrapper<PrefilledOrder> wrapper) {
        if (StringUtils.isNotEmpty((prefilledOrder.getPrefilledOrderId()))) {
            wrapper.eq(PrefilledOrder::getPrefilledOrderId, prefilledOrder.getPrefilledOrderId());
        }
        // 对于商户自己来说，默认只查询自己的数据，因此不需要
        // 但是要适配超级管理员，因此需要
        if (StringUtils.isNotEmpty(prefilledOrder.getMchNo())) {
            wrapper.eq(PrefilledOrder::getMchNo, prefilledOrder.getMchNo());
        }
        if (StringUtils.isNotEmpty(prefilledOrder.getAppId())) {
            wrapper.eq(PrefilledOrder::getAppId, prefilledOrder.getAppId());
        }
        if (prefilledOrder.getStatus() != null) {
            wrapper.eq(PrefilledOrder::getStatus, prefilledOrder.getStatus());
        }
        if (StringUtils.isNotEmpty(prefilledOrder.getSubject())) {
            wrapper.like(PrefilledOrder::getSubject, prefilledOrder.getSubject());
        }
        if (StringUtils.isNotEmpty(prefilledOrder.getBody())) {
            wrapper.like(PrefilledOrder::getBody, prefilledOrder.getBody());
        }
        if (paramJSON != null) {
            if (StringUtils.isNotEmpty(paramJSON.getString("usageStart"))) {
                wrapper.ge(PrefilledOrder::getStartTime, paramJSON.getString("usageStart"));
            }
            if (StringUtils.isNotEmpty(paramJSON.getString("usageEnd"))) {
                wrapper.le(PrefilledOrder::getEndTime, paramJSON.getString("usageEnd"));
            }
        }

        // 按创建时间倒序排列
        wrapper.orderByDesc(PrefilledOrder::getCreatedAt);

        return page(iPage, wrapper);
    }

    public String generatePrefilledOrderId() {
        return generatePrefilledOrderId("PREF");
    }

    public String generatePrefilledOrderId(String prefixedOrderId) {
        return prefixedOrderId + "_" + System.currentTimeMillis();
    }
}
