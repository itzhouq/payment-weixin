package cn.itzhouq.payment.weixin.service.impl;

import cn.itzhouq.payment.weixin.config.WxPayConfig;
import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.enums.OrderStatus;
import cn.itzhouq.payment.weixin.enums.wxpay.WxApiType;
import cn.itzhouq.payment.weixin.enums.wxpay.WxNotifyType;
import cn.itzhouq.payment.weixin.service.OrderInfoService;
import cn.itzhouq.payment.weixin.service.PaymentInfoService;
import cn.itzhouq.payment.weixin.service.WxPayService;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付Service实现类
 *
 * @author itzhouq
 * @date 2022/1/15 11:11
 */
@Service
@Slf4j
public class WxPayServiceImpl implements WxPayService {

    @Resource
    private WxPayConfig wxPayConfig;

    @Resource
    private CloseableHttpClient wxPayClient;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private PaymentInfoService paymentInfoService;

    /**
     * @param productId 商品ID
     * @return code_url 和 订单号
     * @Description 创建订单，调用Native支付接口
     * @author itzhouq
     * @Date 2022/1/15 11:30
     */
    @Override
    public Map<String, Object> nativePay(Long productId) throws Exception {
        log.info("生成订单");

        // 生成订单
        OrderInfo orderInfo = orderInfoService.createOrderByProductId(productId);
        String codeUrl = orderInfo.getCodeUrl();
        if (orderInfo != null && !StringUtils.isEmpty(codeUrl)) {
            log.info("订单已存在，二维码已保存");
            // 返回二维码
            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        }

        log.info("调用统一下单API");
        // 调用统一下单API
        HttpPost httpPost = new HttpPost(wxPayConfig.getDomain().concat(WxApiType.NATIVE_PAY.getType()));
        // 请求body参数
        Gson gson = new Gson();
        Map paramsMap = new HashMap();
        paramsMap.put("appid", wxPayConfig.getAppid());
        paramsMap.put("mchid", wxPayConfig.getMchId());
        paramsMap.put("description", orderInfo.getTitle());
        paramsMap.put("out_trade_no", orderInfo.getOrderNo());
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.NATIVE_NOTIFY.getType()));
        Map amountMap = new HashMap();
        amountMap.put("total", orderInfo.getTotalFee());
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        //将参数转换成json字符串
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数：" + jsonParams);
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        //完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            //响应体
            String bodyAsString = EntityUtils.toString(response.getEntity());
            //响应状态码
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                //处理成功
                log.info("成功, 返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("成功");
            } else {
                log.info("Native下单失败,响应码 = " + statusCode + ",返回结果 = " + bodyAsString);
                throw new IOException("request failed");
            }
            //响应结果
            Map<String, String> resultMap = gson.fromJson(bodyAsString, HashMap.class);
            // 二维码
            codeUrl = resultMap.get("code_url");
            // 保存二维码
            String orderNo = orderInfo.getOrderNo();
            orderInfoService.saveCodeUrl(orderNo, codeUrl);

            Map<String, Object> map = new HashMap<>();
            map.put("codeUrl", codeUrl);
            map.put("orderNo", orderInfo.getOrderNo());
            return map;
        } finally {
            response.close();
        }
    }

    /**
     * @param bodyMap 请求体参数
     * @Description 处理订单
     * @author itzhouq
     * @Date 2022/1/16 22:50
     */
    @Override
    public void processOrder(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("处理订单");

        String plainText = decryptFromResource(bodyMap);

        // 转换明文:https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml
        Gson gson = new Gson();
        Map<String, Object> plainTextMap = gson.fromJson(plainText, HashMap.class);
        String orderNo = (String) plainTextMap.get("out_trade_no");

        // 更新订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

        // 记录支付日志
        paymentInfoService.createPaymentInfo(plainText);
    }

    /**
     * @param bodyMap 请求体数据
     * @return {@link java.lang.String}
     * @Description 密文解密
     * 支付通知-密文解密：https://pay.weixin.qq.com/wiki/doc/apiv3/apis/chapter3_4_5.shtml
     * @author itzhouq
     * @Date 2022/1/16 22:54
     */
    private String decryptFromResource(Map<String, Object> bodyMap) throws GeneralSecurityException {
        log.info("密文解密");

        // 通知数据
        Map<String, String> resourceMap = (Map<String, String>) bodyMap.get("resource");
        // 数据密文
        String ciphertext = resourceMap.get("ciphertext");
        // 随机串
        String nonce = resourceMap.get("nonce");
        // 附加数据
        String associatedData = resourceMap.get("associated_data");
        log.info("密文 ===> {}", ciphertext);
        AesUtil aesUtil = new AesUtil(wxPayConfig.getApiV3Key().getBytes(StandardCharsets.UTF_8));
        String plainText = aesUtil.decryptToString(associatedData.getBytes(StandardCharsets.UTF_8),
                nonce.getBytes(StandardCharsets.UTF_8),
                ciphertext);
        log.info("明文 ===> {}", plainText);

        return plainText;
    }
}
