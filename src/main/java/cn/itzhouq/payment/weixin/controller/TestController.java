package cn.itzhouq.payment.weixin.controller;

import cn.itzhouq.payment.weixin.config.WxPayConfig;
import cn.itzhouq.payment.weixin.vo.R;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 测试
 *
 * @author itzhouq
 * @date 2022/1/14 07:13
 */
@Api(tags = "测试控制器")
@RestController
@RequestMapping("/api/test")
public class TestController {
    @Resource
    private WxPayConfig wxPayConfig;

    @GetMapping
    public R getWxPayConfig() {
        String mchId = wxPayConfig.getMchId();
        return R.ok().data("mchId", mchId);
    }
}
