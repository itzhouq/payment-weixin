package cn.itzhouq.payment.weixin.controller;

import cn.itzhouq.payment.weixin.entity.Product;
import cn.itzhouq.payment.weixin.service.ProductService;
import cn.itzhouq.payment.weixin.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 测试
 *
 * @author itzhouq
 * @date 2022/1/12 23:09
 */
@Api(tags = "商品管理")
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Resource
    private ProductService productService;

    @ApiOperation("测试接口")
    @GetMapping("/test")
    public R test() {
        return R.ok().data("message", "hello").data("date", new Date());
    }

    @GetMapping("/list")
    public R list() {
        List<Product> list = productService.list();
        return R.ok().data("productList", list);
    }
}
