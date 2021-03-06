package org.dullbird.seata.order.demo.service;

import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.dubbo.config.annotation.Reference;
import org.dullbird.seata.demo.account.api.AccountService;
import org.dullbird.seata.demo.order.api.OrderService;
import org.dullbird.seata.demo.product.api.ProductService;
import org.dullbird.seata.order.demo.dao.OrderDao;
import org.dullbird.seata.order.demo.entity.OrderDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author dullBird
 * @version 1.0.0
 * @createTime 2020年05月24日 13:20:00
 */
@org.apache.dubbo.config.annotation.Service
public class OrderServiceImpl implements OrderService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private OrderDao orderDao;

    @Reference
    private AccountService accountService;
    @Reference
    private ProductService productService;

    @Override
    @GlobalTransactional // <1>
    public Integer createOrder(Long userId, Long productId, Integer price) throws Exception {
        Integer amount = 1; // 购买数量，暂时设置为 1。

        logger.info("[createOrder] 当前 XID: {}", RootContext.getXID());

        // <2> 扣减库存
        productService.reduceStock(productId, amount);

        // <3> 扣减余额
        accountService.reduceBalance(userId, price);

        // <4> 保存订单
        OrderDO order = new OrderDO().setUserId(userId).setProductId(productId).setPayAmount(amount * price);
        orderDao.saveOrder(order);
        logger.info("[createOrder] 保存订单: {}", order.getId());

        // 返回订单编号
        return order.getId();
    }

    @Override
    @GlobalTransactional
    public Integer createOrder(Long userId, Long productId, Integer price, Long time) throws Exception {
        Integer order = createOrder(userId, productId, price);

        Thread.sleep(time);
        //让先操作的事务失败回滚
        if (time > 5000) {
            throw new IllegalArgumentException("睡眠时间长的事务失败！");
        }
        return order;
    }

}
