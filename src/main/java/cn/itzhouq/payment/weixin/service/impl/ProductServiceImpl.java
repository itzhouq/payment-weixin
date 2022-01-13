package cn.itzhouq.payment.weixin.service.impl;


import cn.itzhouq.payment.weixin.entity.Product;
import cn.itzhouq.payment.weixin.mapper.ProductMapper;
import cn.itzhouq.payment.weixin.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

}
