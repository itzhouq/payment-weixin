package cn.itzhouq.payment.weixin.controller;

import cn.itzhouq.payment.weixin.service.WxPayService;
import cn.itzhouq.payment.weixin.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

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
     * @Description 调用统一下单API，生成支付二维码
     * 原理解释：https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_0.shtml
     * @param productId 商品ID
     * @return {@link cn.itzhouq.payment.weixin.vo.R}
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

}
