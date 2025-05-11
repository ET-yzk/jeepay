package com.jeequan.jeepay.mch.ctrl.prefilledorder;

import com.alibaba.fastjson.JSONObject;
import com.jeequan.jeepay.core.entity.MchApp;
import com.jeequan.jeepay.core.entity.PayOrder;
import com.jeequan.jeepay.mch.ctrl.CommonCtrl;
import com.jeequan.jeepay.mch.websocket.server.WsPayOrderServer;
import com.jeequan.jeepay.service.impl.MchAppService;
import com.jeequan.jeepay.service.impl.PrefilledOrderService;
import com.jeequan.jeepay.util.JeepayKit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 预填订单公开支付页 - 回调通知处理
 *
 * @author zkye
 * @version 1.0
 * @see <a href=""></a>
 * @since 2025/5/7
 */
@Tag(name = "用户侧预填订单类")
@RestController
@RequestMapping("/api/anon/prefilledOrder/notify")
public class PrefilledOrderPublicPayNotifyController extends CommonCtrl {

    @Autowired private MchAppService mchAppService;
    @Autowired private PrefilledOrderService prefilledOrderService; // 注入 PrefilledOrderService

    @Operation(summary = "预填订单支付回调信息")
    @Parameters({
            @Parameter(name = "appId", description = "应用ID", required = true),
            @Parameter(name = "mchNo", description = "商户号", required = true),
            @Parameter(name = "sign", description = "签名值", required = true),
            @Parameter(name = "payOrderId", description = "支付订单号", required = true),
            @Parameter(name = "state", description = "支付状态", required = true),
            @Parameter(name = "errCode", description = "错误代码"),
            @Parameter(name = "errMsg", description = "错误信息"),
            @Parameter(name = "extParam", description = "扩展参数，应包含 sourcePrefilledOrderId")
    })
    @RequestMapping("/payOrder")
    public void payOrderNotify() throws IOException {

        //请求参数
        JSONObject params = getReqParamJSON();
        logger.info("接收到预填订单支付回调通知: {}", params.toJSONString());

        String mchNo = params.getString("mchNo");
        String appId = params.getString("appId");
        String sign = params.getString("sign");
        MchApp mchApp = mchAppService.getById(appId);
        if(mchApp == null || !mchApp.getMchNo().equals(mchNo)){
            logger.warn("回调通知应用校验失败, appId: {}, mchNo: {}", appId, mchNo);
            response.getWriter().print("app is not exists");
            return;
        }

        // 移除签名参数进行验签
        String signValue = params.remove("sign").toString(); // 保存签名值，因为JeepayKit.getSign会修改params
        if(!JeepayKit.getSign(params, mchApp.getAppSecret()).equalsIgnoreCase(signValue)){
            logger.warn("回调通知验签失败, appId: {}, mchNo: {}", appId, mchNo);
            response.getWriter().print("sign fail");
            return;
        }
        params.put("sign", signValue); // 验签成功后，如果后续逻辑需要原始sign，可以再放回去

        // 处理业务逻辑，例如更新订单状态、增加使用次数等
        int paymentState = params.getIntValue("state");
        String payOrderId = params.getString("payOrderId");

        if (PayOrder.STATE_SUCCESS == paymentState) { // 仅在支付成功时处理
            logger.info("预填订单支付成功, payOrderId: {}", payOrderId);
            String sourcePrefilledOrderId = null;
//            String channelExtraStr = params.getString("channelExtra");
            String extParam = params.getString("extParam");

            if (StringUtils.isNotEmpty(extParam)) {
                try {
                    JSONObject extraJson = JSONObject.parseObject(extParam);
                    sourcePrefilledOrderId = extraJson.getString("sourcePrefilledOrderId");
                } catch (Exception e) {
                    logger.error("解析 extParam 出错, extParam: {}, payOrderId: {}", extParam, payOrderId, e);
                }
            }

            if (StringUtils.isNotEmpty(sourcePrefilledOrderId)) {
                boolean incremented = prefilledOrderService.incrementUsageCountAndCheck(sourcePrefilledOrderId);
                if (incremented) {
                    logger.info("预填订单 {} 使用次数已成功增加, 关联支付单号: {}", sourcePrefilledOrderId, payOrderId);
                } else {
                    // incrementUsageCountAndCheck 方法内部已经记录了详细日志
                    logger.warn("预填订单 {} 使用次数增加失败 (可能已达上限或订单问题), 关联支付单号: {}", sourcePrefilledOrderId, payOrderId);
                    // 此处可能需要根据业务需求进行额外处理，例如发出警告通知等
                }
            } else {
                logger.error("支付回调中未能获取 sourcePrefilledOrderId, 无法更新预填订单使用次数. payOrderId: {}, extParam: {}", payOrderId, extParam);
                // 这是一个严重问题，需要排查为何 sourcePrefilledOrderId 未能传递到回调
            }
        } else {
            logger.info("预填订单支付状态非成功 (state={}), payOrderId: {}. 不更新使用次数.", paymentState, payOrderId);
        }


        JSONObject msg = new JSONObject();
        msg.put("state", params.getIntValue("state"));
        msg.put("errCode", params.getString("errCode"));
        msg.put("errMsg", params.getString("errMsg"));
        msg.put("payOrderId", payOrderId);

        //推送到前端 - 注意：WsPayOrderServer可能需要替换为预填订单相关的WebSocket服务
        WsPayOrderServer.sendMsgByOrderId(payOrderId, msg.toJSONString());

        response.getWriter().print("SUCCESS");
    }
}
