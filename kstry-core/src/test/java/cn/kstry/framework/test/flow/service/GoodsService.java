package cn.kstry.framework.test.flow.service;

import cn.kstry.framework.core.annotation.*;
import cn.kstry.framework.core.bus.InstructContent;
import cn.kstry.framework.core.bus.ScopeDataNotice;
import cn.kstry.framework.core.bus.ScopeDataOperator;
import cn.kstry.framework.test.flow.bo.Goods;
import cn.kstry.framework.test.flow.bo.Hospital;
import cn.kstry.framework.test.flow.bo.Te4Request;
import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@SuppressWarnings("unused")
@Validated
@TaskComponent(name = "goods_service")
public class GoodsService {

    @TaskService(name = "get_goods")
    public ScopeDataNotice getGoods(@Min(1) @ReqTaskParam("goodsId") Long id) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("商品名称");
        goods.setPrice(20L);
        return ScopeDataNotice.sta().notice("goods", goods);
    }

    @TaskInstruct(name = "goods-fill")
    @TaskService(name = "fill_goods", invoke = @Invoke(demotion = "pr:goods_service@XXX"))
    public Mono<Void> fillGoods(@NotNull @StaTaskParam("goods") Goods goods, Hospital hospital, InstructContent instructContent) {
        if (instructContent != null) {
            Assert.assertNotNull(instructContent.getContent());
            Assert.assertNotNull(instructContent.getInstruct());
        }
        goods.setHospital(hospital);
        System.out.println("thread ->" + Thread.currentThread().getName() + ", fill goods ->" + JSON.toJSONString(goods));
        return Mono.empty();
    }

    @TaskService(name = "say_request")
    public void sayRequest(@NotNull ScopeDataOperator operator) {
        Te4Request request = operator.getReqScope();
        request.increase();
        request.getNodeList().add(operator.getTaskProperty().orElse(null));
    }

    @TaskService(name = "check_params")
    public void checkParams(@Valid Goods goods) {
        assert goods != null && goods.getId() != null;
    }
}
