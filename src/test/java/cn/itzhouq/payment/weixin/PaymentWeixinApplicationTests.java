package cn.itzhouq.payment.weixin;

import cn.itzhouq.payment.weixin.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.security.PrivateKey;

@SpringBootTest
class PaymentWeixinApplicationTests {

    @Resource
    private WxPayConfig wxPayConfig;

//    /**
//     * @Description 测试获取商户私钥
//     * @author itzhouq
//     * @Date 2022/1/14 07:33
//     */
//    @Test
//    void testGetPrivateKey() {
//
//        // 获取私钥路径
//        String privateKeyPath = wxPayConfig.getPrivateKeyPath();
//
//        // 获取私钥
//        PrivateKey privateKey = wxPayConfig.getPrivateKey(privateKeyPath);
//        System.out.println(privateKey);
//
//    }

}
