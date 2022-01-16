package cn.itzhouq.payment.weixin.service.impl;


import cn.itzhouq.payment.weixin.entity.PaymentInfo;
import cn.itzhouq.payment.weixin.enums.PayType;
import cn.itzhouq.payment.weixin.mapper.PaymentInfoMapper;
import cn.itzhouq.payment.weixin.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

    /**
     * @param plainText 明文字符串
     * @Description 记录支付日志
     * @author itzhouq
     * @Date 2022/1/16 23:22
     */
    @Override
    public void createPaymentInfo(String plainText) {
        log.info("记录支付日志");

        Gson gson = new Gson();
        Map<String, Object> plainTextMap = gson.fromJson(plainText, HashMap.class);

        // 订单号
        String orderNo = (String) plainTextMap.get("out_trade_no");
        // 业务编号
        String transactionId = (String) plainTextMap.get("transaction_id");
        // 支付类型
        String tradeType = (String) plainTextMap.get("trade_type");
        // 支付状态
        String tradeState = (String) plainTextMap.get("trade_state");
        // 用户实际支付金额
        Map<String, Object> amount = (Map) plainTextMap.get("amount");
        Integer payerTotal = ((Double) amount.get("payer_total")).intValue();

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderNo(orderNo);
        paymentInfo.setPaymentType(PayType.WXPAY.getType());
        paymentInfo.setTransactionId(transactionId);
        paymentInfo.setTradeType(tradeType);
        paymentInfo.setTradeState(tradeState);
        paymentInfo.setPayerTotal(payerTotal);
        paymentInfo.setContent(plainText);

        baseMapper.insert(paymentInfo);
    }
}
