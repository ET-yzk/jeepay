package com.jeequan.jeepay.mch.ctrl.prefilledorder;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.service.impl.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final PayWayService payWayService;
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
        this.payWayService = payWayService;
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

        // todo 若app不可用则不返回数据
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

        IPage<PrefilledOrder> pages = prefilledOrderService.listByPage(getIPage(), prefilledOrder, paramJSON, wrapper);

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
        if (prefilledOrder == null || !prefilledOrder.getMchNo().equals(getCurrentMchNo())) {
            return ApiRes.fail(ApiCodeEnum.SYS_OPERATION_FAIL_SELETE);
        }

        return ApiRes.ok(prefilledOrder);
    }

}
