package cn.itzhouq.payment.weixin.service.impl;


import cn.itzhouq.payment.weixin.entity.PaymentInfo;
import cn.itzhouq.payment.weixin.mapper.PaymentInfoMapper;
import cn.itzhouq.payment.weixin.service.PaymentInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class PaymentInfoServiceImpl extends ServiceImpl<PaymentInfoMapper, PaymentInfo> implements PaymentInfoService {

}
