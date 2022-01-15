package cn.itzhouq.payment.weixin.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一返回结果
 *
 * @author itzhouq
 * @date 2022/1/13 21:12
 */
@Data
@Accessors(chain = true)
public class R {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 数据对象
     */
    private Map<String, Object> data = new HashMap<>();

    public static R ok() {
        R r = new R();
        r.setCode(0);
        r.setMessage("成功");
        return r;
    }

    public static R error() {
        R r = new R();
        r.setCode(-1);
        r.setMessage("失败");
        return r;
    }
    public R data(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
