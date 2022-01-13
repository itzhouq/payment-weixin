package cn.itzhouq.payment.weixin.service.impl;

import cn.itzhouq.payment.weixin.entity.RefundInfo;
import cn.itzhouq.payment.weixin.mapper.RefundInfoMapper;
import cn.itzhouq.payment.weixin.service.RefundInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {

}
