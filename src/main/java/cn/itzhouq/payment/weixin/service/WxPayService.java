package cn.itzhouq.payment.weixin.service;

import java.util.Map;

/**
 * 微信支付Service
 *
 * @author itzhouq
 * @date 2022/1/15 11:11
 */
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;
}
