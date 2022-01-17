package cn.itzhouq.payment.weixin.service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * 微信支付Service
 *
 * @author itzhouq
 * @date 2022/1/15 11:11
 */
public interface WxPayService {
    Map<String, Object> nativePay(Long productId) throws Exception;

    void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException;

    void cancelOrder(String orderNo) throws IOException;

    String queryOrder(String orderNo) throws IOException;
}
