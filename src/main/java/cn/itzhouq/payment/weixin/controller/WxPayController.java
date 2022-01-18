package cn.itzhouq.payment.weixin.controller;

import cn.itzhouq.payment.weixin.service.WxPayService;
import cn.itzhouq.payment.weixin.util.HttpUtils;
import cn.itzhouq.payment.weixin.util.WechatPay2ValidatorForRequest;
import cn.itzhouq.payment.weixin.vo.R;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付
 *
 * @author itzhouq
 * @date 2022/1/15 11:08
 */
@CrossOrigin // 跨域
@RestController
@RequestMapping("/api/wx-pay")
@Api(tags = "网站微信支付")
@Slf4j
public class WxPayController {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private Verifier verifier;

    /**
     * @param productId 商品ID
     * @return {@link cn.itzhouq.payment.weixin.vo.R}
     * @Description 调用统一下单API，生成支付二维码
     * 原理解释：https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_0.shtml
     * @author itzhouq
     * @Date 2022/1/15 12:30
     */
    @ApiOperation("调用统一下单API，生成支付二维码")
    @PostMapping("native/{productId}")
    public R nativePay(@PathVariable Long productId) throws Exception {
        log.info("发起支付请求");

        // 返回支付二维码连接和订单号
        Map<String, Object> map = wxPayService.nativePay(productId);

        return R.ok().setData(map);
    }

    /**
     * @param request  请求
     * @param response 响应
     * @return {@link java.lang.String}
     * @Description 支付通知：微信支付通过支付通知接口将用户支付成功消息通知给商户
     * 支付通知文档：https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml
     * 测试场景：
     * 1. 通知成功
     * 2. 通知失败
     * 3. 通知超时
     * @author itzhouq
     * @Date 2022/1/15 21:11
     */
    @ApiOperation("支付通知")
    @PostMapping("/native/notify")
    public String nativeNotify(HttpServletRequest request, HttpServletResponse response) {
        Gson gson = new Gson();
        // 应答参数
        Map<String, String> map = new HashMap<>();

        try {
            // 处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的id ===> {}", requestId);
            log.info("支付通知的完整数据 ===> {}", body);

            // 签名的验证：
            // https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_1.shtml
            // https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml
            WechatPay2ValidatorForRequest validator = new WechatPay2ValidatorForRequest(verifier, body, requestId);
            if (!validator.validate(request)) {
                log.error("通知验签失败");
                // 失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }

            log.info("通知验签成功");
            // 处理订单--密文解密
            wxPayService.processOrder(bodyMap);

            // 测试超时应答：添加睡眠时间使应答超时
            // 设置响应超时，可以接收到微信支付的重复的支付结果通知
            // 通知重复，数据库会记录多余的处理日志
//             TimeUnit.SECONDS.sleep(5);
            // 测试错误应答
            // int i = 9 / 0;

            // 成功应答，成功应答必须为200或者204，否则就是失败应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            // 测试错误应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "系统错误");
            return gson.toJson(map);
        }
    }

    @ApiOperation("用户取消订单")
    @PostMapping("/cancel/{orderNo}")
    public R cancel(@PathVariable("orderNo") String orderNo) throws Exception {
        log.info("取消订单");
        wxPayService.cancelOrder(orderNo);
        return R.ok().setMessage("订单已取消");
    }

    @ApiOperation("查询订单：测试订单状态用")
    @GetMapping("/query/{orderNo}")
    public R queryOrder(@PathVariable String orderNo) throws Exception {
        log.info("查询订单");

        String bodyAsString = wxPayService.queryOrder(orderNo);
        return R.ok().setMessage("查询成功").data("bodyAsString", bodyAsString);
    }

    @ApiOperation("申请退款")
    @PostMapping("/refunds/{orderNo}/{reason}")
    public R refunds(@PathVariable String orderNo, @PathVariable String reason) throws IOException {
        log.info("申请退款");
        wxPayService.refund(orderNo, reason);
        return R.ok();
    }

    @ApiOperation("查询退款：测试用")
    @GetMapping("/query-refund/{refundNo}")
    public R queryRefund(@PathVariable String refundNo) throws IOException {
        log.info("查询退款");

        String result = wxPayService.queryRefund(refundNo);
        return R.ok().setMessage("查询成功").data("result", result);
    }

    /**
     * @param request  请求
     * @param response 响应
     * @return {@link java.lang.String}
     * @Description 退款结果通知
     * 退款状态改变后，微信会把相关退款结果发给商户
     * @author itzhouq
     * @Date 2022/1/18 16:11
     */
    @PostMapping("/refunds/notify")
    public String refundsNotify(HttpServletRequest request, HttpServletResponse response) {
        log.info("退款通知执行");

        Gson gson = new Gson();
        Map<String, String> map = new HashMap<>();

        try {
            // 处理通知参数
            String body = HttpUtils.readData(request);
            Map<String, Object> bodyMap = gson.fromJson(body, HashMap.class);
            String requestId = (String) bodyMap.get("id");
            log.info("支付通知的ID ===> {}", request);

            // 签名的验证
            WechatPay2ValidatorForRequest wechatPay2ValidatorForRequest = new WechatPay2ValidatorForRequest(verifier, requestId, body);
            if (!wechatPay2ValidatorForRequest.validate(request)) {
                log.error("通知验签失败");
                // 失败应答
                response.setStatus(500);
                map.put("code", "ERROR");
                map.put("message", "通知验签失败");
                return gson.toJson(map);
            }

            log.info("通知验签成功");

            // 处理退款单
            wxPayService.processRefund(bodyMap);

            // 成功应答
            response.setStatus(200);
            map.put("code", "SUCCESS");
            map.put("message", "成功");
            return gson.toJson(map);
        } catch (Exception e) {
            e.printStackTrace();
            // 失败应答
            response.setStatus(500);
            map.put("code", "ERROR");
            map.put("message", "失败");
            return gson.toJson(map);
        }
    }

    @ApiOperation("获取账单url：测试用")
    @GetMapping("/querybill/{billDate}/{type}")
    public R queryTradeBill(@PathVariable String billDate, @PathVariable String type) throws IOException {
        log.info("获取账单url");

        String downloadUrl = wxPayService.queryBill(billDate, type);
        return R.ok().setMessage("获取账单url成功").data("downloadUrl", downloadUrl);
    }

    @ApiOperation("下载账单")
    @GetMapping("/downloadbill/{billDate}/{type}")
    public R downloadBill(@PathVariable String billDate, @PathVariable String type) throws IOException {
        log.info("下载账单");

        String result = wxPayService.downloadBill(billDate, type);
        return R.ok().data("result", result);
    }


}
