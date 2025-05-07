package com.jeequan.jeepay.mch.ctrl.prefilledorder;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.JeepayClient;
import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.MchPayPassage;
import com.jeequan.jeepay.core.entity.PrefilledOrder;
import com.jeequan.jeepay.core.exception.BizException;
import com.jeequan.jeepay.core.model.ApiRes;
import com.jeequan.jeepay.core.model.DBApplicationConfig;
import com.jeequan.jeepay.exception.JeepayException;
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.model.PayOrderCreateReqModel;
import com.jeequan.jeepay.request.PayOrderCreateRequest;
import com.jeequan.jeepay.response.PayOrderCreateResponse;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.impl.MchPayPassageService;
import com.jeequan.jeepay.service.impl.PrefilledOrderService;
import com.jeequan.jeepay.service.impl.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * 预填订单公开支付页控制器
 *
 * @author zkye
 * @version 1.0
 * @since 2025/5/6
 */
@Tag(name = "预填订单公开支付页")
@RestController
@RequestMapping("/api/anon/prefilledOrder/publicPay")
public class PrefilledOrderPublicPayController extends CommonCtrl {

    @Autowired private PrefilledOrderService prefilledOrderService;
    @Autowired private MchAppService mchAppService;
    @Autowired private MchPayPassageService mchPayPassageService;
    @Autowired private SysConfigService sysConfigService;

    /** 查询预填订单信息 **/
    @Operation(summary = "查询预填订单信息")
    @Parameters({
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true)
    })
    @GetMapping("/{prefilledOrderId}")
    public ApiRes detail(@PathVariable("prefilledOrderId") String prefilledOrderId) {
        // 1. 查询预填订单
        PrefilledOrder prefilledOrder = prefilledOrderService.getById(prefilledOrderId);
        if (prefilledOrder == null || prefilledOrder.getIsDeleted() == CS.IS_DELETED) {
            throw new BizException("预填订单不存在");
        }

        // 2. 校验订单状态
        if (prefilledOrder.getStatus() != CS.PUB_USABLE) {
            throw new BizException("预填订单已禁用");
        }

        // 3. 校验订单有效期
        Date now = new Date();
        if (prefilledOrder.getStartTime() != null && now.before(prefilledOrder.getStartTime())) {
            throw new BizException("预填订单未到生效时间");
        }
        if (prefilledOrder.getEndTime() != null && now.after(prefilledOrder.getEndTime())) {
            throw new BizException("预填订单已过期");
        }

        // 4. 校验使用次数
        if (prefilledOrder.getMaxUsageCount() != null && 
            prefilledOrder.getCurrentUsageCount() >= prefilledOrder.getMaxUsageCount()) {
            throw new BizException("预填订单已达到最大使用次数");
        }

        return ApiRes.ok(prefilledOrder);
    }

    /** 查询预填订单可用支付方式 **/
    @Operation(summary = "查询预填订单可用支付方式")
    @Parameters({
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true)
    })
    @GetMapping("/{prefilledOrderId}/payways")
    public ApiRes<Set<String>> payWayList(@PathVariable("prefilledOrderId") String prefilledOrderId) {
        // 1. 查询预填订单
        PrefilledOrder prefilledOrder = prefilledOrderService.getById(prefilledOrderId);
        if (prefilledOrder == null || prefilledOrder.getIsDeleted() == CS.IS_DELETED) {
            throw new BizException("预填订单不存在");
        }

        // 2. 查询有效支付方式
        Set<String> payWaySet = new HashSet<>();
        mchPayPassageService.list(
                MchPayPassage.gw().select(MchPayPassage::getWayCode)
                        .eq(MchPayPassage::getMchNo, prefilledOrder.getMchNo())
                        .eq(MchPayPassage::getAppId, prefilledOrder.getAppId())
                        .eq(MchPayPassage::getState, CS.PUB_USABLE)
        ).forEach(r -> payWaySet.add(r.getWayCode()));

        return ApiRes.ok(payWaySet);
    }

    /** 创建支付订单 **/
    @Operation(summary = "创建支付订单")
    @Parameters({
            @Parameter(name = "prefilledOrderId", description = "预填订单号", required = true),
            @Parameter(name = "wayCode", description = "支付方式代码", required = true),
            @Parameter(name = "generalRemark", description = "普通备注"),
            @Parameter(name = "invoiceRemark", description = "发票备注")
    })
    @PostMapping("/{prefilledOrderId}/pay")
    public ApiRes doPay(@PathVariable("prefilledOrderId") String prefilledOrderId) {
        // 1. 获取请求参数
        String wayCode = getValStringRequired("wayCode");
        String generalRemark = getValString("generalRemark");
        String invoiceRemark = getValString("invoiceRemark");
        String payDataType = getValString("payDataType");

        // 2. 查询预填订单
        PrefilledOrder prefilledOrder = prefilledOrderService.getById(prefilledOrderId);
        if (prefilledOrder == null || prefilledOrder.getIsDeleted() == CS.IS_DELETED) {
            throw new BizException("预填订单不存在");
        }

        // 3. 校验订单状态
        if (prefilledOrder.getStatus() != CS.PUB_USABLE) {
            throw new BizException("预填订单已禁用");
        }

        // 4. 校验订单有效期
        Date now = new Date();
        if (prefilledOrder.getStartTime() != null && now.before(prefilledOrder.getStartTime())) {
            throw new BizException("预填订单未到生效时间");
        }
        if (prefilledOrder.getEndTime() != null && now.after(prefilledOrder.getEndTime())) {
            throw new BizException("预填订单已过期");
        }

        // 5. 校验使用次数
        if (prefilledOrder.getMaxUsageCount() != null && 
            prefilledOrder.getCurrentUsageCount() >= prefilledOrder.getMaxUsageCount()) {
            throw new BizException("预填订单已达到最大使用次数");
        }

        // 6. 校验备注信息
        if (prefilledOrder.getRemarkConfig() != null) {
            if (prefilledOrder.getRemarkConfig().getGeneralEnabled() && 
                prefilledOrder.getRemarkConfig().getGeneralRequired() && 
                StringUtils.isEmpty(generalRemark)) {
                throw new BizException("普通备注为必填项");
            }
            if (prefilledOrder.getRemarkConfig().getInvoiceEnabled() && 
                prefilledOrder.getRemarkConfig().getInvoiceRequired() && 
                StringUtils.isEmpty(invoiceRemark)) {
                throw new BizException("发票备注为必填项");
            }
        }

        // 7. 获取商户应用信息
        MchApp mchApp = mchAppService.getById(prefilledOrder.getAppId());
        if(mchApp == null || mchApp.getState() != CS.PUB_USABLE){
            throw new BizException("商户应用不存在或不可用");
        }

        // 8. 创建支付订单请求
        PayOrderCreateRequest request = new PayOrderCreateRequest();
        PayOrderCreateReqModel model = new PayOrderCreateReqModel();
        request.setBizModel(model);

        model.setMchNo(prefilledOrder.getMchNo());
        model.setAppId(prefilledOrder.getAppId());
        model.setMchOrderNo(prefilledOrderService.generatePrefilledOrderId("PRE"));
        model.setWayCode(wayCode);
        model.setAmount(prefilledOrder.getAmount());
        model.setCurrency(prefilledOrder.getCurrency());
        model.setClientIp(getClientIp());
        model.setSubject(prefilledOrder.getSubject());
        model.setBody(prefilledOrder.getBody());

        // 设置回调地址
        DBApplicationConfig dbApplicationConfig = sysConfigService.getDBApplicationConfig();
        model.setNotifyUrl(dbApplicationConfig.getMchSiteUrl() + "/api/anon/prefilledOrder/notify/payOrder");

        // 设置扩展参数
        JSONObject extParams = new JSONObject();
        if(StringUtils.isNotEmpty(generalRemark)) {
            extParams.put("generalRemark", generalRemark.trim());
        }
        if(StringUtils.isNotEmpty(invoiceRemark)) {
            extParams.put("invoiceRemark", invoiceRemark.trim());
        }
        // 记录来源预填订单ID
        extParams.put("sourcePrefilledOrderId", prefilledOrderId);
        model.setChannelExtra(extParams.toString());

        // 9. 调用支付接口
        JeepayClient jeepayClient = new JeepayClient(dbApplicationConfig.getPaySiteUrl(), mchApp.getAppSecret());
        try {
            PayOrderCreateResponse response = jeepayClient.execute(request);
            if(response.getCode() != 0){
                throw new BizException(response.getMsg());
            }
            return ApiRes.ok(response.get());
        } catch (JeepayException e) {
            throw new BizException(e.getMessage());
        }
    }
}
