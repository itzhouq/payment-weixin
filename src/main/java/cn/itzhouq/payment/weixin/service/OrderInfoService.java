package cn.itzhouq.payment.weixin.service;


import cn.itzhouq.payment.weixin.entity.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId);

    void saveCodeUrl(String orderNo, String codeUrl);
}
