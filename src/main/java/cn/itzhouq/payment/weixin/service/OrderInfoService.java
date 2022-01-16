package cn.itzhouq.payment.weixin.service;


import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.enums.OrderStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OrderInfoService extends IService<OrderInfo> {

    OrderInfo createOrderByProductId(Long productId);

    void saveCodeUrl(String orderNo, String codeUrl);

    List<OrderInfo> listOrderByCreateTimeDesc();

    void updateStatusByOrderNo(String orderNo, OrderStatus success);
}
