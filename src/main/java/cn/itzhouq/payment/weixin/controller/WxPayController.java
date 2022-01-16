package cn.itzhouq.payment.weixin.controller;

import cn.itzhouq.payment.weixin.service.WxPayService;
import cn.itzhouq.payment.weixin.util.HttpUtils;
import cn.itzhouq.payment.weixin.vo.R;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
     *  1. 通知成功
     *  2. 通知失败
     *  3. 通知超时
     *
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
            log.info("支付通知的id ===> {}", bodyMap.get("id"));
            log.info("支付通知的完整数据 ===> {}", body);

            // TODO 签名的验证
            // TODO 处理订单

            // 测试超时应答：添加睡眠时间使应答超时
            // TimeUnit.SECONDS.sleep(5);
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


}
