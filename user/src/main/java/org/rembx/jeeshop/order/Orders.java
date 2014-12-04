package org.rembx.jeeshop.order;

import org.apache.commons.collections.CollectionUtils;
import org.rembx.jeeshop.mail.Mailer;
import org.rembx.jeeshop.order.model.Order;
import org.rembx.jeeshop.order.model.OrderItem;
import org.rembx.jeeshop.order.model.OrderStatus;
import org.rembx.jeeshop.role.JeeshopRoles;
import org.rembx.jeeshop.user.MailTemplateFinder;
import org.rembx.jeeshop.user.UserFinder;
import org.rembx.jeeshop.user.model.Address;
import org.rembx.jeeshop.user.model.UserPersistenceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Orders resource.
 */
@Path("orders")
@Stateless
public class Orders {

    private final static Logger LOG = LoggerFactory.getLogger(Orders.class);

    @PersistenceContext(unitName = UserPersistenceUnit.NAME)
    private EntityManager entityManager;

    @Inject
    private OrderFinder orderFinder;

    @Inject
    private UserFinder userFinder;


    @Inject
    private Mailer mailer;

    @Inject
    private MailTemplateFinder mailTemplateFinder;

    @Resource
    private SessionContext sessionContext;

    public Orders() {
    }

    public Orders(EntityManager entityManager, OrderFinder orderFinder, UserFinder userFinder,
                  MailTemplateFinder mailTemplateFinder, Mailer mailer, SessionContext sessionContext) {
        this.entityManager = entityManager;
        this.orderFinder = orderFinder;
        this.userFinder = userFinder;
        this.mailTemplateFinder = mailTemplateFinder;
        this.mailer = mailer;
        this.sessionContext = sessionContext;
    }


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({JeeshopRoles.USER, JeeshopRoles.ADMIN})
    public Order create(Order order, @QueryParam("userLogin")String userLogin) {
        if (order .getId() != null || (order.getDeliveryAddress() != null && order.getDeliveryAddress().getId() != null)
                || (order.getBillingAddress()!=null && order.getBillingAddress().getId() !=null)){
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        if (sessionContext.isCallerInRole(JeeshopRoles.USER)){
            order.setUser(userFinder.findByLogin(sessionContext.getCallerPrincipal().getName()));
        }

        if (sessionContext.isCallerInRole(JeeshopRoles.ADMIN)){
            order.setUser(userFinder.findByLogin(userLogin));
        }


        if (CollectionUtils.isNotEmpty(order.getItems())){
            order.getItems().forEach(orderItem -> {
                if (orderItem.getId()!=null)
                    throw new WebApplicationException(Response.Status.BAD_REQUEST);
                orderItem.setOrder(order);
            });
        }

        order.setStatus(OrderStatus.CREATED);

        entityManager.persist(order);

        return order;
    }

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(JeeshopRoles.ADMIN)
    public List<Order> findAll(@QueryParam("search") String search, @QueryParam("start") Integer start, @QueryParam("size") Integer size, @QueryParam("orderBy") String orderBy, @QueryParam("isDesc") Boolean isDesc) {
        if (search != null)
            return orderFinder.findBySearchCriteria(search, start, size, orderBy, isDesc);
        else
            return orderFinder.findAll(start, size, orderBy, isDesc);
    }


    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed(JeeshopRoles.ADMIN)
    public Long count(@QueryParam("search") String search) {
        if (search != null)
            return orderFinder.countBySearchCriteria(search);
        else
            return orderFinder.countAll();
    }

}