package cn.itzhouq.payment.weixin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("cn.itzhouq.payment.weixin.mapper")
public class PaymentWeixinApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentWeixinApplication.class, args);
    }

}
