package cn.itzhouq.payment.weixin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;


/**
 * MybatisPlus配置
 *
 * @author itzhouq
 * @date 2022/1/13 21:45
 */
@Configuration
@MapperScan("cn.itzhouq.payment.weixin.mapper")
@EnableTransactionManagement //启用事务管理
public class MybatisPlusConfig {
}
