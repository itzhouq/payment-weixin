package cn.itzhouq.payment.weixin.service.impl;

import cn.itzhouq.payment.weixin.config.WxPayConfig;
import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.entity.RefundInfo;
import cn.itzhouq.payment.weixin.enums.OrderStatus;
import cn.itzhouq.payment.weixin.enums.wxpay.WxApiType;
import cn.itzhouq.payment.weixin.enums.wxpay.WxNotifyType;
import cn.itzhouq.payment.weixin.enums.wxpay.WxTradeState;
import cn.itzhouq.payment.weixin.service.OrderInfoService;
import cn.itzhouq.payment.weixin.service.PaymentInfoService;
import cn.itzhouq.payment.weixin.service.RefundInfoService;
import cn.itzhouq.payment.weixin.service.WxPayService;
import com.google.gson.Gson;
import com.wechat.pay.contrib.apache.httpclient.util.AesUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

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

    private final ReentrantLock lock = new ReentrantLock();

    @Resource
    private RefundInfoService refundInfoService;

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

        // 在对业务数据进行状态检查和处理之前，要采用数据锁进行并发控制，以避免函数重入造成的数据混乱。
        // 尝试获取锁
        // 成功获取则立即返回true，获取失败则立刻返回false，不必一直等待锁的释放
        if (lock.tryLock()) {
            try {
                // 处理重复通知
                // 保证接口调用的幂等性：无论接口被调用多少次，产生的结果是一致的
                String orderStatus = orderInfoService.getOrderStatus(orderNo);
                if (!Objects.equals(OrderStatus.NOTPAY.getType(), orderStatus)) {
                    return;
                }

                // 模拟通知并发
//        try {
//            TimeUnit.SECONDS.sleep(5);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

                // 更新订单状态
                orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);

                // 记录支付日志
                paymentInfoService.createPaymentInfo(plainText);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * @param orderNo 订单号
     * @Description 用户取消订单
     * @author itzhouq
     * @Date 2022/1/18 07:23
     */
    @Override
    public void cancelOrder(String orderNo) throws IOException {
        // 调用微信支付的关单接口
        this.closeOrder(orderNo);

        // 更新商户端的订单状态
        orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CANCEL);

    }

    /**
     * @param orderNo 订单号
     * @return {@link java.lang.String}
     * @Description 查单接口调用
     * @author itzhouq
     * @Date 2022/1/18 07:40
     */
    @Override
    public String queryOrder(String orderNo) throws IOException {
        log.info("查单接口调用 ===> {}", orderNo);

        String url = String.format(WxApiType.ORDER_QUERY_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url).concat("?mchid=").concat(wxPayConfig.getMchId());

        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        // 完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            //响应体
            int statusCode = response.getStatusLine().getStatusCode();
            //响应状态码
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
            return bodyAsString;
        } finally {
            response.close();
        }
    }

    /**
     * @param orderNo 订单号
     * @Description 根据订单号查询微信支付查单接口，核实订单状态
     * 如果订单已支付，则更新商户端订单状态，并记录支付日志
     * 如果订单未支付，则调用关单接口关闭订单，并更新商户端订单状态
     * @author itzhouq
     * @Date 2022/1/18 08:25
     */
    @Override
    public void checkOrderStatus(String orderNo) throws IOException {
        log.warn("根据订单号合适订单状态 ===> {}", orderNo);

        // 调用微信支付查单接口
        String result = this.queryOrder(orderNo);

        Gson gson = new Gson();
        Map resultMap = gson.fromJson(result, HashMap.class);

        // 获取微信支付端的订单状态
        Object tradeState = resultMap.get("trade_state");

        // 判断订单状态
        if (Objects.equals(WxTradeState.SUCCESS.getType(), tradeState)) {
            log.warn("核实订单已支付 ===> {}", orderNo);

            // 如果订单确认已支付则更新本地订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.SUCCESS);
            // 记录日志
            paymentInfoService.createPaymentInfo(result);
        }

        if (Objects.equals(WxTradeState.NOTPAY.getType(), tradeState)) {
            log.warn("核实订单未支付 ===> {}", orderNo);

            // 如果订单未支付，则调用关单接口
            this.closeOrder(orderNo);

            // 更新本地订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.CLOSED);
        }
    }

    /**
     * @param orderNo 订单号
     * @param reason  退款原因
     * @Description 退款
     * @author itzhouq
     * @Date 2022/1/18 14:20
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void refund(String orderNo, String reason) throws IOException {
        log.info("创建退款单记录");
        // 根据订单编号创建退款单
        RefundInfo refundInfo = refundInfoService.createRefundByOrderNo(orderNo, reason);

        log.info("调用退款API");

        // 调用统一下单API
        String url = wxPayConfig.getDomain().concat(WxApiType.DOMESTIC_REFUNDS.getType());
        HttpPost httpPost = new HttpPost(url);

        // 请求body参数
        Gson gson = new Gson();
        Map paramsMap = new HashMap();
        // 订单编号
        paramsMap.put("out_trade_no", orderNo);
        // 退款单编号
        paramsMap.put("out_refund_no", refundInfo.getRefundNo());
        paramsMap.put("reason", reason);
        // 退款通知地址
        paramsMap.put("notify_url", wxPayConfig.getNotifyDomain().concat(WxNotifyType.REFUND_NOTIFY.getType()));

        Map amountMap = new HashMap();
        // 退款金额
        amountMap.put("refund", refundInfo.getRefund());
        amountMap.put("total", refundInfo.getTotalFee());
        // 退款币种
        amountMap.put("currency", "CNY");
        paramsMap.put("amount", amountMap);

        // 将参数转换为JSON字符串
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数 ===> {}", paramsMap);

        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        // 设置请求报文格式
        entity.setContentType("application/json");
        // 将请求报文放入请求对象
        httpPost.setEntity(entity);
        // 设置响应报文格式
        httpPost.setHeader("Accept", "application/json");

        // 完成签名并执行请求，并完成验签
        CloseableHttpResponse response = wxPayClient.execute(httpPost);

        try {
            // 解析响应结果
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功，退款返回结果 =" + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException("退款异常，响应码 = " + statusCode + ", 退款返回结果 = " + bodyAsString);
            }

            // 更新订单状态
            orderInfoService.updateStatusByOrderNo(orderNo, OrderStatus.REFUND_PROCESSING);

            // 更新退款单
            refundInfoService.updateRefund(bodyAsString);
        } finally {
            response.close();
        }


    }

    /**
     * @param refundNo 退款单号
     * @return {@link java.lang.String}
     * @Description 查询退款接口调用
     * @author itzhouq
     * @Date 2022/1/18 15:18
     */
    @Override
    public String queryRefund(String refundNo) throws IOException {
        log.info("查询退款接口调用 ===> {}", refundNo);

        String url = String.format(WxApiType.DOMESTIC_REFUNDS_QUERY.getType(), refundNo);
        url = wxPayConfig.getDomain().concat(url);

        // 创建远程GET请求对象
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("Accept", "application/json");

        // 完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpGet);

        try {
            String bodyAsString = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                log.info("成功，查询退款返回结果 = " + bodyAsString);
            } else if (statusCode == 204) {
                log.info("成功");
            } else {
                throw new RuntimeException("查询退款异常，响应码 = " + statusCode + ", 查询退款返回结果 = " + bodyAsString);
            }
            return bodyAsString;
        } finally {
            response.close();
        }
    }

    /**
     * @param orderNo 订单号
     * @Description 关单接口的调用
     * @author itzhouq
     * @Date 2022/1/18 07:25
     */
    private void closeOrder(String orderNo) throws IOException {
        log.info("关单接口的调用，订单号 ===> {}", orderNo);
        // 创建远程请求对象
        String url = String.format(WxApiType.CLOSE_ORDER_BY_NO.getType(), orderNo);
        url = wxPayConfig.getDomain().concat(url);
        HttpPost httpPost = new HttpPost(url);

        // 组装JSON请求体
        Gson gson = new Gson();
        Map<String, String> paramsMap = new HashMap<>();
        paramsMap.put("mchid", wxPayConfig.getMchId());
        String jsonParams = gson.toJson(paramsMap);
        log.info("请求参数 ===> {}", jsonParams);
        //将请求参数设置到请求对象中
        StringEntity entity = new StringEntity(jsonParams, "utf-8");
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        httpPost.setHeader("Accept", "application/json");
        // 完成签名并执行请求
        CloseableHttpResponse response = wxPayClient.execute(httpPost);
        try {
            int statusCode = response.getStatusLine().getStatusCode();
            //响应状态码
            if (statusCode == 200) {
                //处理成功
                log.info("成功200");
            } else if (statusCode == 204) {
                //处理成功，无返回Body
                log.info("成功204");
            } else {
                log.info("Native下单失败,响应码 = " + statusCode);
                throw new IOException("request failed");
            }
        } finally {
            response.close();
        }
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
