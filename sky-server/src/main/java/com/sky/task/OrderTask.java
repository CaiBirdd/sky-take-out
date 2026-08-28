package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 自定义定时任务，实现订单状态定时处理
 */
@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    /**
     * 处理支付超时订单
     */
    //测试用，每5秒执行一次
    //@Scheduled(cron = "0/5 * * * * ?")
    //每10分钟执行一次
    @Scheduled(cron = "0 */10 * * * ?")
    public void processTimeoutOrder(){
        log.info("处理支付超时订单：{}", new Date());
        // 查询支付超时的订单 从当前时间往前推15分钟，查询订单状态为待支付的订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> orders = orderMapper.getByStatusAndOrdertimeLT(Orders.PENDING_PAYMENT, time);
        if(orders != null && orders.size() > 0){
            for (Orders order : orders) {
                // 修改订单状态为已取消
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("支付超时，订单已取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    /**
     * 处理“派送中”状态的订单
     */
    //测试用 每10s执行一次
    //@Scheduled(cron = "0/10 * * * * ?")
    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("处理派送中订单：{}", new Date());
        // 查询“派送中”状态的订单，从当前时间往前推60分钟，查询订单状态为“派送中”的订单
        LocalDateTime time = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getByStatusAndOrdertimeLT(Orders.DELIVERY_IN_PROGRESS, time);
        if(orders != null && orders.size() > 0){
            for (Orders order : orders) {
                // 修改订单状态为已完成
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
