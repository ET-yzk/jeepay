package com.jeequan.jeepay.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.PrefilledOrder;
import com.jeequan.jeepay.service.mapper.PrefilledOrderMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

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

    private final static Logger logger = LoggerFactory.getLogger(PrefilledOrderService.class);

    /**
     * 公开分页查询预填订单
     * @param iPage 分页参数
     * @param wrapper mybatis-plus 查询条件构造器
     * @return 分页结果
     */
    public IPage<PrefilledOrder> annoListByPage(IPage iPage, LambdaQueryWrapper<PrefilledOrder> wrapper) {
        // 指定只查询以下字段
        wrapper.select(
            PrefilledOrder::getPrefilledOrderId,
            PrefilledOrder::getAmount,
            PrefilledOrder::getSubject,
            PrefilledOrder::getBody,
            PrefilledOrder::getStartTime,
            PrefilledOrder::getEndTime
        );

        Date utc_date = new Date();
        wrapper.eq(PrefilledOrder::getStatus, CS.PUB_USABLE) // 启用状态
                .and(w -> w.le(PrefilledOrder::getStartTime, utc_date).or().isNull(PrefilledOrder::getStartTime))// 开始时间小于等于当前时间或为空
                .and(w -> w.ge(PrefilledOrder::getEndTime, utc_date).or().isNull(PrefilledOrder::getEndTime)); // 结束时间大于等于当前时间或为空

        // 按创建时间倒序排列
        wrapper.orderByDesc(PrefilledOrder::getCreatedAt);

        return page(iPage, wrapper);
    }

    /**
     * 分页查询预填订单
     * @param iPage 分页参数
     * @param prefilledOrder 查询条件
     * @param paramJSON 其他查询参数 (如时间范围)
     * @param wrapper mybatis-plus 查询条件构造器
     * @return 分页结果
     */
    public IPage<PrefilledOrder> listByPage(IPage iPage, PrefilledOrder prefilledOrder, JSONObject paramJSON, LambdaQueryWrapper<PrefilledOrder> wrapper) {
        // 指定只查询以下字段
        wrapper.select(
            PrefilledOrder::getPrefilledOrderId,
            PrefilledOrder::getAmount,
            PrefilledOrder::getStatus,
            PrefilledOrder::getSubject,
            PrefilledOrder::getStartTime,
            PrefilledOrder::getEndTime,
            PrefilledOrder::getRemarkConfig,
            PrefilledOrder::getCurrentUsageCount,
            PrefilledOrder::getMaxUsageCount,
            PrefilledOrder::getOverSoldCount
        );

        if (StringUtils.isNotEmpty((prefilledOrder.getPrefilledOrderId()))) {
            wrapper.eq(PrefilledOrder::getPrefilledOrderId, prefilledOrder.getPrefilledOrderId());
        }
        // 对于商户自己来说，默认只查询自己的数据，因此不需要
        // 但是要适配超级管理员，因此需要对商户号进行过滤
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
                wrapper.and(w -> w.ge(PrefilledOrder::getStartTime, paramJSON.getString("usageStart"))
                        .or()
                        .isNull(PrefilledOrder::getStartTime));
            }
            if (StringUtils.isNotEmpty(paramJSON.getString("usageEnd"))) {
                wrapper.and(w -> w.le(PrefilledOrder::getEndTime, paramJSON.getString("usageEnd"))
                        .or()
                        .isNull(PrefilledOrder::getEndTime));
            }

            if (StringUtils.isNotEmpty(paramJSON.getString("description"))) {
                wrapper.and(w -> w.like(PrefilledOrder::getSubject, paramJSON.getString("description"))
                        .or()
                        .like(PrefilledOrder::getBody, paramJSON.getString("description")));
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

    /**
     * 原子性地增加预填订单的使用次数。
     * 只有在当前使用次数小于最大使用次数，或者最大使用次数未设置（NULL，表示无限）时，才会增加。
     *
     * @param prefilledOrderId 预填订单ID
     * @return 如果成功增加计数，则返回 true；否则返回 false（例如，已达到限制或订单不存在）。
     */
    @Transactional // 建议添加事务注解
    public boolean incrementUsageCountAndCheck(String prefilledOrderId) {
        if (prefilledOrderId == null) {
            logger.warn("Attempted to increment usage count with null prefilledOrderId.");
            return false;
        }

        LambdaUpdateWrapper<PrefilledOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
            .eq(PrefilledOrder::getPrefilledOrderId, prefilledOrderId)
            // 条件：max_usage_count 为 NULL (无限制) 或者 current_usage_count < max_usage_count
            .apply("(max_usage_count IS NULL OR current_usage_count < max_usage_count)")
            .setSql("current_usage_count = current_usage_count + 1");

        boolean updated = this.update(updateWrapper);

        if (updated) {
            logger.info("Successfully incremented usage count for prefilled order ID: {}", prefilledOrderId);
        } else {
            // 查询以确定失败原因（可选，用于更详细的日志）
            PrefilledOrder order = this.getOne(
                PrefilledOrder.gw()
                        .eq(PrefilledOrder::getPrefilledOrderId, prefilledOrderId)
                        .select(PrefilledOrder::getCurrentUsageCount, PrefilledOrder::getMaxUsageCount)
            );
            if (order == null) {
                logger.warn("Failed to increment usage count: Prefilled order ID {} not found.", prefilledOrderId);
            } else if (order.getMaxUsageCount() != null && order.getCurrentUsageCount() >= order.getMaxUsageCount()) {
                logger.warn("Failed to increment usage count for prefilled order ID: {}. Usage limit already reached (current: {}, max: {}).",
                    prefilledOrderId, order.getCurrentUsageCount(), order.getMaxUsageCount());

                // 原子性增加 overSoldCount
                LambdaUpdateWrapper<PrefilledOrder> overSellWrapper = new LambdaUpdateWrapper<>();
                overSellWrapper
                        .eq(PrefilledOrder::getPrefilledOrderId, prefilledOrderId)
                        .setSql("`over_sold_count` = `over_sold_count` + 1");

                updated = this.update(overSellWrapper);
            } else {
                logger.warn("Failed to increment usage count for prefilled order ID: {}. Unknown reason or condition not met.", prefilledOrderId);
            }
        }
        return updated;
    }
}
