package com.jeequan.jeepay.mch.ctrl.prefilledorder;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jeequan.jeepay.core.aop.MethodLog;
import com.jeequan.jeepay.core.constants.ApiCodeEnum;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PrefilledOrder;
import com.jeequan.jeepay.core.entity.RemarkConfig;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiPageRes;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.model.DBApplicationConfig; // 确保引入
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 预填订单类
 *
 * @author zkye
 * @version 1.0
 * @see <a href="">无</a>
 * @since 2025/4/23
 */
@Tag(name = "预填订单类")
@RestController
@RequestMapping("/api/prefilledOrder")
public class PrefilledOrderController extends CommonCtrl {

    private final PrefilledOrderService prefilledOrderService;
    //private final PayWayService payWayService;
    private final MchAppService mchAppService;
    private final SysConfigService sysConfigService;
    private final MchPayPassageService mchPayPassageService;

    @Autowired
    public PrefilledOrderController(
            PrefilledOrderService prefilledOrderService,
            PayWayService payWayService,
            MchAppService mchAppService,
            SysConfigService sysConfigService,
            MchPayPassageService mchPayPassageService) {
        this.prefilledOrderService = prefilledOrderService;
        //this.payWayService = payWayService;
        this.mchAppService = mchAppService;
        this.sysConfigService = sysConfigService;
        this.mchPayPassageService = mchPayPassageService;
    }

    /** 查询商户对应应用下支持的支付方式 **/
    @Operation(summary = "查询商户对应应用下支持的支付方式")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "appId", description = "应用ID", required = true)
    })
    @PreAuthorize("hasAuthority('ENT_PREFILLED_ORDER_PAYWAY_LIST')")
    @GetMapping("/payWays/{appId}")
    public ApiRes<Set<String>> payWayList(@PathVariable("appId") String appId) {
        // 1. 校验app是否存在且可用
        MchApp mchApp = mchAppService.getById(appId);
        if (mchApp == null || !mchApp.getMchNo().equals(getCurrentMchNo()) || mchApp.getState() != CS.PUB_USABLE) {
            throw new BizException("商户应用不存在或已禁用");
        }

        // 2. 查询有效支付方式，返回空：支付方式 未启用/未配置
        Set<String> payWaySet = new HashSet<>();
        mchPayPassageService.list(
                MchPayPassage.gw().select(MchPayPassage::getWayCode)
                        .eq(MchPayPassage::getMchNo, getCurrentMchNo())
                        .eq(MchPayPassage::getAppId, appId)
                        .eq(MchPayPassage::getState, CS.PUB_USABLE)
        ).forEach(r -> payWaySet.add(r.getWayCode()));

        return ApiRes.ok(payWaySet);
    }

    /**
     * 查询预填订单列表
     */
    @Operation(summary = "预填订单列表")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "pageNumber", description = "分页页码"),
            @Parameter(name = "pageSize", description = "分页条数"),
            @Parameter(name = "usageStart", description = "日期格式字符串（yyyy-MM-dd HH:mm:ss），时间范围查询--开始时间，查询范围：大于等于此时间"),
            @Parameter(name = "usageEnd", description = "日期格式字符串（yyyy-MM-dd HH:mm:ss），时间范围查询--结束时间，查询范围：小于等于此时间"),
            @Parameter(name = "prefilledOrderId", description = "预填订单号"),
            @Parameter(name = "appId", description = "应用ID"),
            @Parameter(name = "status", description = "状态: 0-禁用, 1-启用"),
            @Parameter(name = "subject", description = "订单标题"),
            @Parameter(name = "body", description = "订单描述信息")
    })
    @PreAuthorize("hasAuthority('ENT_PREFILLED_ORDER_LIST')")
    @GetMapping
    public ApiPageRes<PrefilledOrder> list() {

        PrefilledOrder prefilledOrder = getObject(PrefilledOrder.class);
        JSONObject paramJSON = getReqParamJSON();

        LambdaQueryWrapper<PrefilledOrder> wrapper = PrefilledOrder.gw();
        wrapper.eq(PrefilledOrder::getMchNo, getCurrentMchNo());
        wrapper.eq(PrefilledOrder::getIsDeleted, CS.IS_NOT_DELETED);

        IPage<PrefilledOrder> pages = prefilledOrderService.listByPage(getIPage(), prefilledOrder, paramJSON, wrapper);

        // 获取商户站点URL，用于拼接支付页地址
        DBApplicationConfig dbApplicationConfig = sysConfigService.getDBApplicationConfig();
        String prefilledOrderPublicPaySiteUrl = dbApplicationConfig.getPrefilledOrderPublicPaySiteUrl();

        if (pages != null && pages.getRecords() != null) {
            for (PrefilledOrder order : pages.getRecords()) {
                if (StringUtils.isNotEmpty(order.getPrefilledOrderId())) {
                    order.setPublicPayUrl(prefilledOrderPublicPaySiteUrl + "/prefilledOrder/publicPay/" + order.getPrefilledOrderId());
                    // 对商户不显示逻辑删除标记
                    order.setIsDeleted(null);
                }
            }
        }

        return ApiPageRes.pages(pages);
    }

    /**
     * 创建预填订单
     */
    @Operation(summary = "创建预填订单")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "appId", description = "应用ID", required = true),
            @Parameter(name = "subject", description = "订单标题", required = true),
            @Parameter(name = "body", description = "订单描述信息"),
            @Parameter(name = "amount", description = "支付金额", required = true),
            @Parameter(name = "currency", description = "三位货币代码,人民币:cny"),
            @Parameter(name = "remark_config", description = "备注配置"),
            @Parameter(name = "status", description = "状态: 0-禁用, 1-启用"),
            @Parameter(name = "start_time", description = "生效时间"),
            @Parameter(name = "end_time", description = "失效时间"),
            @Parameter(name = "max_usage_count", description = "最大成功支付次数")
    })
    @PreAuthorize("hasAuthority('ENT_PREFILLED_ORDER_ADD')")
    @MethodLog(remark = "创建预填订单")
    @PostMapping
    public ApiRes create(@Valid @RequestBody PrefilledOrder prefilledOrder) {
        // 如果用户没传 remarkConfig，使用默认
        RemarkConfig cfg = prefilledOrder.getRemarkConfig();
        if (cfg == null) {
            cfg = RemarkConfig.defaultConfig();
            prefilledOrder.setRemarkConfig(cfg);
        }
        // 触发业务规则校验
        try {
            cfg.validate();
        } catch (IllegalArgumentException e) {
            return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, e.getMessage());
        }

        MchApp mchApp = mchAppService.getById(prefilledOrder.getAppId());
        if(mchApp == null || mchApp.getState() != CS.PUB_USABLE || !mchApp.getAppId().equals(prefilledOrder.getAppId())){
            throw new BizException("商户应用不存在或不可用");
        }

        // 设置商户号
        prefilledOrder.setMchNo(getCurrentMchNo());
        // 生成预填订单ID
        prefilledOrder.setPrefilledOrderId(prefilledOrderService.generatePrefilledOrderId());
        // 保存到数据库
        boolean saveResult = prefilledOrderService.save(prefilledOrder);

        if (saveResult) {
            return ApiRes.ok(prefilledOrder);
        } else {
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "创建预填订单失败");
        }
    }

    /**
     * 查询预填订单详情
     */
    @Operation(summary = "查询预填订单详情")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true)
    })
    @PreAuthorize("hasAnyAuthority('ENT_PREFILLED_ORDER_VIEW', 'ENT_PREFILLED_ORDER_ADD')")
    @GetMapping("/{prefilledOrderId}")
    public ApiRes detail(@PathVariable("prefilledOrderId") String prefilledOrderId) {
        PrefilledOrder prefilledOrder = prefilledOrderService.getById(prefilledOrderId);

        // 校验订单是否存在且属于当前商户
        if (prefilledOrder == null || !prefilledOrder.getMchNo().equals(getCurrentMchNo()) || prefilledOrder.getIsDeleted() == CS.IS_DELETED) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        // 获取商户站点URL，用于拼接支付页地址
        DBApplicationConfig dbApplicationConfig = sysConfigService.getDBApplicationConfig();
        String prefilledOrderPublicPaySiteUrl = dbApplicationConfig.getPrefilledOrderPublicPaySiteUrl();

        if (StringUtils.isNotEmpty(prefilledOrder.getPrefilledOrderId())) {
            prefilledOrder.setPublicPayUrl(prefilledOrderPublicPaySiteUrl + "/prefilledOrder/publicPay/" + prefilledOrder.getPrefilledOrderId());
        }

        // 对商户不显示逻辑删除标记
        prefilledOrder.setIsDeleted(null);

        return ApiRes.ok(prefilledOrder);
    }

    /**
     * 修改预填订单
     */
    @Operation(summary = "修改预填订单")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true, in = ParameterIn.PATH),
            @Parameter(name = "subject", description = "订单标题"),
            @Parameter(name = "body", description = "订单描述信息"),
            @Parameter(name = "amount", description = "支付金额"),
            @Parameter(name = "currency", description = "三位货币代码,人民币:cny"),
            @Parameter(name = "remark_config", description = "备注配置"),
            @Parameter(name = "status", description = "状态: 0-禁用, 1-启用"),
            @Parameter(name = "start_time", description = "生效时间"),
            @Parameter(name = "end_time", description = "失效时间"),
            @Parameter(name = "max_usage_count", description = "最大成功支付次数")
    })
    @PreAuthorize("hasAuthority('ENT_PREFILLED_ORDER_EDIT')")
    @MethodLog(remark = "修改预填订单")
    @PutMapping("/{prefilledOrderId}")
    public ApiRes update(@PathVariable("prefilledOrderId") String prefilledOrderId, @RequestBody PrefilledOrder prefilledOrder) {

        // 1. 校验订单id
        if (StringUtils.isEmpty(prefilledOrderId)) {
            return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, "预填订单号不能为空");
        }

        // 2. 校验备注业务规则
        RemarkConfig cfg = prefilledOrder.getRemarkConfig();
        if (cfg != null) {
            // 触发业务规则校验
            try {
                cfg.validate();
            } catch (IllegalArgumentException e) {
                return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, e.getMessage());
            }
        }

        // 3.查询原订单
        PrefilledOrder dbPrefilledOrder = prefilledOrderService.getById(prefilledOrderId);
        if (dbPrefilledOrder == null || !dbPrefilledOrder.getMchNo().equals(getCurrentMchNo())) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE); // 订单不存在或不属于当前商户
        }

        // 4. 构建动态更新条件（仅更新非空字段）
        LambdaUpdateWrapper<PrefilledOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PrefilledOrder::getPrefilledOrderId, prefilledOrderId);

        // 动态添加更新字段
        if (StringUtils.isNotBlank(prefilledOrder.getAppId())) {
            updateWrapper.set(PrefilledOrder::getAppId, prefilledOrder.getAppId());
        }
        if (StringUtils.isNotBlank(prefilledOrder.getSubject())) {
            updateWrapper.set(PrefilledOrder::getSubject, prefilledOrder.getSubject());
        }
        if (prefilledOrder.getAmount() != null) {
            updateWrapper.set(PrefilledOrder::getAmount, prefilledOrder.getAmount());
        }
        if (StringUtils.isNotBlank(prefilledOrder.getCurrency())) {
            updateWrapper.set(PrefilledOrder::getCurrency, prefilledOrder.getCurrency());
        }
        if (StringUtils.isNotBlank(prefilledOrder.getBody())) {
            updateWrapper.set(PrefilledOrder::getBody, prefilledOrder.getBody());
        }
        if (prefilledOrder.getRemarkConfig() != null) {
            updateWrapper.set(PrefilledOrder::getRemarkConfig, prefilledOrder.getRemarkConfig());
        }
        if (prefilledOrder.getStatus() != null) {
            updateWrapper.set(PrefilledOrder::getStatus, prefilledOrder.getStatus());
        }
        if (prefilledOrder.getStartTime() != null) {
            updateWrapper.set(PrefilledOrder::getStartTime, prefilledOrder.getStartTime());
        }
        if (prefilledOrder.getEndTime() != null) {
            updateWrapper.set(PrefilledOrder::getEndTime, prefilledOrder.getEndTime());
        }
        if (prefilledOrder.getMaxUsageCount() != null) {
            updateWrapper.set(PrefilledOrder::getMaxUsageCount, prefilledOrder.getMaxUsageCount());
        }

        // 5. 执行更新
        boolean updateResult = prefilledOrderService.update(updateWrapper);

        if (!updateResult) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_UPDATE); // 更新失败
        }

        // todo 是否需要推送修改预填订单消息
        return ApiRes.ok();
    }

    /**
     * 删除预填订单 (逻辑删除)
     */
    @Operation(summary = "删除预填订单")
    @Parameters({
            @Parameter(name = "iToken", description = "用户身份凭证", required = true, in = ParameterIn.HEADER),
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true, in = ParameterIn.PATH)
    })
    @PreAuthorize("hasAuthority('ENT_PREFILLED_ORDER_DEL')")
    @DeleteMapping("/{prefilledOrderId}")
    public ApiRes delete(@PathVariable("prefilledOrderId") String prefilledOrderId) {
        // 校验参数
        if (StringUtils.isEmpty(prefilledOrderId)) {
            return ApiRes.fail(ApiCodeEnum.PARAMS_ERROR, "预填订单号不能为空");
        }

        // 查询订单
        PrefilledOrder dbPrefilledOrder = prefilledOrderService.getById(prefilledOrderId);

        // 校验订单是否存在且属于当前商户
        if (dbPrefilledOrder == null || !dbPrefilledOrder.getMchNo().equals(getCurrentMchNo())) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        // 逻辑删除：更新 is_deleted 字段
        LambdaUpdateWrapper<PrefilledOrder> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PrefilledOrder::getPrefilledOrderId, prefilledOrderId)
                     .eq(PrefilledOrder::getMchNo, getCurrentMchNo())
                     .set(PrefilledOrder::getIsDeleted, CS.IS_DELETED); // CS.IS_DELETED = 1

        boolean removeResult = prefilledOrderService.update(updateWrapper);

        if (removeResult) {
            return ApiRes.ok();
        } else {
            return ApiRes.fail(ApiCodeEnum.SYSTEM_ERROR, "删除预填订单失败");
        }
    }
}
