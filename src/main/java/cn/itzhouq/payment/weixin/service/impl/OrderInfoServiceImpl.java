package cn.itzhouq.payment.weixin.service.impl;


import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.entity.Product;
import cn.itzhouq.payment.weixin.enums.OrderStatus;
import cn.itzhouq.payment.weixin.mapper.OrderInfoMapper;
import cn.itzhouq.payment.weixin.mapper.ProductMapper;
import cn.itzhouq.payment.weixin.service.OrderInfoService;
import cn.itzhouq.payment.weixin.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {
    @Resource
    private ProductMapper productMapper;

    /**
     * @param productId 商品ID
     * @return {@link cn.itzhouq.payment.weixin.entity.OrderInfo}
     * @Description 生成订单
     * @author itzhouq
     * @Date 2022/1/15 19:10
     */
    @Override
    public OrderInfo createOrderByProductId(Long productId) {
        // 查找已存在但未支付的订单
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId);
        if (orderInfo != null) {
            return orderInfo;
        }

        // 获取商品信息
        Product product = productMapper.selectById(productId);

        // 生成订单
        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        // 金额：单位为分
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        baseMapper.insert(orderInfo);
        return orderInfo;
    }

    /**
     * @param orderNo 订单号
     * @param codeUrl 订单二维码
     * @Description 存储订单二维码
     * @author itzhouq
     * @Date 2022/1/15 19:49
     */
    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        baseMapper.update(orderInfo, queryWrapper);
    }

    /**
     * @param productId 商品ID
     * @return {@link cn.itzhouq.payment.weixin.entity.OrderInfo}
     * @Description 根据商品ID查询未支付订单, 防止重复创建订单
     * @author itzhouq
     * @Date 2022/1/15 19:15
     */
    private OrderInfo getNoPayOrderByProductId(Long productId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
//        queryWrapper.eq("user_id", userId);
        return baseMapper.selectOne(queryWrapper);
    }
}
