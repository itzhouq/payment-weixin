package cn.itzhouq.payment.weixin.service.impl;

import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.entity.RefundInfo;
import cn.itzhouq.payment.weixin.mapper.RefundInfoMapper;
import cn.itzhouq.payment.weixin.service.OrderInfoService;
import cn.itzhouq.payment.weixin.service.RefundInfoService;
import cn.itzhouq.payment.weixin.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
public class RefundInfoServiceImpl extends ServiceImpl<RefundInfoMapper, RefundInfo> implements RefundInfoService {
    @Resource
    private OrderInfoService orderInfoService;

    /**
     * @param orderNo 订单号
     * @param reason  退款原因
     * @return {@link cn.itzhouq.payment.weixin.entity.RefundInfo}
     * @Description 根据订单号创建退款订单
     * @author itzhouq
     * @Date 2022/1/18 14:11
     */
    @Override
    public RefundInfo createRefundByOrderNo(String orderNo, String reason) {
        // 根据订单号获取订单号信息
        OrderInfo orderInfo = orderInfoService.getOrderInfoByOrderNo(orderNo);

        // 根据订单号生成退款订单
        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setOrderNo(orderNo);
        // 退款单编号
        refundInfo.setRefundNo(OrderNoUtils.getRefundNo());
        // 原订单金额（分）
        refundInfo.setTotalFee(orderInfo.getTotalFee());
        // 退款金额（分）
        refundInfo.setRefund(orderInfo.getTotalFee());
        // 退款原因
        refundInfo.setReason(reason);
        // 保存退款订单
        baseMapper.insert(refundInfo);
        return refundInfo;
    }

    /**
     * @Description 记录退款记录
     * @param content 内容
     * @author itzhouq
     * @Date 2022/1/18 14:16
     */
    @Override
    public void updateRefund(String content) {
        // JSON字符串转换成Map
        Gson gson = new Gson();
        Map<String, String> resultMap = gson.fromJson(content, HashMap.class);

        // 根据退款单编号修改退款单
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("refund_no", resultMap.get("out_refund_no"));

        // 设置要修改的字段
        RefundInfo refundInfo = new RefundInfo();
        // 微信支付退款单号
        refundInfo.setRefundId(resultMap.get("refund_id"));
        // 查询退款和申请退款中返回参数
        if (resultMap.get("status") != null) {
            // 退款状态
            refundInfo.setRefundStatus(resultMap.get("status"));
            // 将全部响应结果存入数据库的content字段
            refundInfo.setContentReturn(content);
        }

        // 退款回调中的回调参数
        if (resultMap.get("refund_status") != null) {
            // 退款状态
            refundInfo.setRefundStatus(resultMap.get("refund_status"));
            // 将全部响应结果存入数据库的content字段
            refundInfo.setContentNotify(content);
        }

        // 更新退款单
        baseMapper.update(refundInfo, queryWrapper);
    }
}
