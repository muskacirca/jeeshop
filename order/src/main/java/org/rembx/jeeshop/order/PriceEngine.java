package org.rembx.jeeshop.order;

import org.rembx.jeeshop.order.model.Order;
import org.rembx.jeeshop.order.model.PaymentInfo;

/**
 * Created by remi on 07/12/14.
 */
public interface PriceEngine {
    public Double computePrice(Order order);
}
