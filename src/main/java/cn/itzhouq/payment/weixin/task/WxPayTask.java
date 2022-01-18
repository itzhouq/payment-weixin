package cn.itzhouq.payment.weixin.task;

import cn.itzhouq.payment.weixin.entity.OrderInfo;
import cn.itzhouq.payment.weixin.entity.RefundInfo;
import cn.itzhouq.payment.weixin.service.OrderInfoService;
import cn.itzhouq.payment.weixin.service.RefundInfoService;
import cn.itzhouq.payment.weixin.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

/**
 * 定时任务
 *
 * @author itzhouq
 * @date 2022/1/18 07:54
 */
@Component
@Slf4j
public class WxPayTask {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private RefundInfoService refundInfoService;

    /**
     * 测试
     * (cron="秒 分 时 日 月 周")
     * *：每隔一秒执行
     * 0/3：从第0秒开始，每隔3秒执行一次
     * 1-3: 从第1秒开始执行，到第3秒结束执行
     * 1,2,3：第1、2、3秒执行
     * ?：不指定，若指定日期，则不指定周，反之同理
     */
    @Scheduled(cron = "0/3 * * * * ?")
    public void task1() {
        log.info("task1 执行");
    }

    /**
     * @Description 定时查找超时订单定时任务
     * 从第0秒开始每隔30秒执行1次，查询创建超过5分钟，并且未支付的订单
     * @author itzhouq
     * @Date 2022/1/18 08:17
     */
    @Scheduled(cron = "0/30 * * * * ?")
    public void orderConfirm() throws IOException {
        log.info("orderConfirm被执行......");

        List<OrderInfo> orderInfoList = orderInfoService.getNoPayOrderDuration(5);
        for (OrderInfo orderInfo : orderInfoList) {
            String orderNo = orderInfo.getOrderNo();
            log.warn("超时订单 ===> {}", orderNo);

            // 核实订单状态：调用微信支付查单接口
            wxPayService.checkOrderStatus(orderNo);
        }
    }

    @Scheduled(cron = "0/30 * * * * ?")
    public void refundConfirm() throws IOException {
        log.info("refundConfirm被执行......");

        // 找出申请退款超过5分钟并且未成功的退款单
        List<RefundInfo> refundInfoList = refundInfoService.getNoRefundOrderByDuration(5);
        for (RefundInfo refundInfo : refundInfoList) {
            String refundNo = refundInfo.getRefundNo();
            log.warn("超时未退款的退款单号 ====> {}", refundNo);

            // 核实订单状态：调用微信支付查询退款接口
            wxPayService.checkRefundStatus(refundNo);
        }
    }
}
