package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOrderDTO;
import com.flyemu.share.dto.SalesOrderItemDTO;
import com.flyemu.share.dto.SalesReportItemDTO;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.repository.SalesOrderItemRepository;
import com.flyemu.share.repository.SalesOrderRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.sagacity.sqltoy.dao.SqlToyLazyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @功能描述: 销售报表
 * @作者: wl
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReportService extends AbsService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;


    public PageResults<SalesReportItemDTO> salesItem(Page page, SalesReportService.Query query) {

        PageResults<SalesReportItemDTO> results = new PageResults<>(new ArrayList<>(),page,0);
        String salesType = query.getSalesType();
        if(StringUtils.equals("all",salesType)){
        }
        if(StringUtils.equals("out",salesType)){
            results = salesOutItem(page, query);
        }
        if(StringUtils.equals("return",salesType)){
            results = salesReturnItem(page,query);
        }
        return results;
    }
    public PageResults<SalesReportItemDTO> salesOutItem(Page page, SalesReportService.Query query) {
        // 获取动态生成的 WHERE 子句和参数
        String whereClause = query.getWhereClause();
        Map<String, Object> params = query.getParams();
        // // 去除开头的 " AND "，确保 WHERE 子句正确
        if (!whereClause.isEmpty()) {
            whereClause = whereClause.substring(5);
        }

        // 查询总记录数
        String countSql = "SELECT COUNT(*) FROM jxc_sales_outbound_item soi " +
                "LEFT JOIN jxc_sales_outbound so ON so.id = soi.sales_outbound_id " +
                "LEFT JOIN jxc_customer c ON c.id = so.customer_id " +
                "LEFT JOIN jxc_warehouse w ON w.id = soi.warehouse_id " +
                "LEFT JOIN jxc_product p ON p.id = soi.product_id " +
                "LEFT JOIN jxc_unit u ON u.id = soi.base_unit_id " +
                "WHERE " + whereClause;

        long totalSize = lazyDao.getCount(countSql, params);

        // 查询分页数据
        String sql = "SELECT soi.*, c.name AS customer_name, w.name AS warehouse_name, p.name AS product_name, " +
                "p.code AS product_code, u.name AS unit_name, so.order_no AS order_no , so.outbound_date AS outboundDate " +
                "FROM jxc_sales_outbound_item soi " +
                "LEFT JOIN jxc_sales_outbound so ON so.id = soi.sales_outbound_id " +
                "LEFT JOIN jxc_customer c ON c.id = so.customer_id " +
                "LEFT JOIN jxc_warehouse w ON w.id = soi.warehouse_id " +
                "LEFT JOIN jxc_product p ON p.id = soi.product_id " +
                "LEFT JOIN jxc_unit u ON u.id = soi.base_unit_id " +
                "WHERE " + whereClause +
                " ORDER BY soi.id DESC " +
                "LIMIT :limit OFFSET :offset";

        // 添加分页参数
        params.put("limit", page.getOffsetEnd());
        params.put("offset", page.getOffset());

        // 执行查询并映射结果
        List<SalesReportItemDTO> dtos = lazyDao.findBySql(sql, params, SalesReportItemDTO.class);

        return new PageResults<>(dtos, page, totalSize);
    }

    public PageResults<SalesReportItemDTO> salesReturnItem(Page page, SalesReportService.Query query) {
        // 获取动态生成的 WHERE 子句和参数
        String whereClause = query.getWhereClause();

        Map<String, Object> params = query.getParams();
        // // 去除开头的 " AND "，确保 WHERE 子句正确
        if (!whereClause.isEmpty()) {
            whereClause = whereClause.substring(5);
        }

        //退货单修改时间查询字段
        whereClause = whereClause.replaceAll("outbound_date", "return_date");

        // 查询总记录数
        String countSql = "SELECT COUNT(*) FROM jxc_sales_return_item sri " +
                "LEFT JOIN jxc_sales_return so ON so.id = sri.sales_return_id " +
                "LEFT JOIN jxc_customer c ON c.id = so.customer_id " +
                "LEFT JOIN jxc_warehouse w ON w.id = sri.warehouse_id " +
                "LEFT JOIN jxc_product p ON p.id = sri.product_id " +
                "LEFT JOIN jxc_unit u ON u.id = sri.base_unit_id " +
                "WHERE " + whereClause;

        long totalSize = lazyDao.getCount(countSql, params);

        // 查询分页数据
        String sql = "SELECT sri.*, c.name AS customer_name, w.name AS warehouse_name, p.name AS product_name, " +
                "p.code AS product_code, u.name AS unit_name, so.order_no AS order_no , so.return_date AS orderDate " +
                "FROM jxc_sales_return_item sri " +
                "LEFT JOIN jxc_sales_return so ON so.id = sri.sales_return_id " +
                "LEFT JOIN jxc_customer c ON c.id = so.customer_id " +
                "LEFT JOIN jxc_warehouse w ON w.id = sri.warehouse_id " +
                "LEFT JOIN jxc_product p ON p.id = sri.product_id " +
                "LEFT JOIN jxc_unit u ON u.id = sri.base_unit_id " +
                "WHERE " + whereClause +
                " ORDER BY sri.id DESC " +
                "LIMIT :limit OFFSET :offset";

        // 添加分页参数
        params.put("limit", page.getOffsetEnd());
        params.put("offset", page.getOffset());

        // 执行查询并映射结果
        List<SalesReportItemDTO> dtos = lazyDao.findBySql(sql, params, SalesReportItemDTO.class);

        return new PageResults<>(dtos, page, totalSize);
    }


    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();
        StringBuilder whereClause = new StringBuilder();
        @Getter
        Map<String, Object> params = new HashMap<>();

        private String salesType;


        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
                whereClause.append(" AND so.merchant_id = :merchantId");
                params.put("merchantId", merchantId);
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
                whereClause.append(" AND so.account_book_id = :accountBookId");
                params.put("accountBookId", accountBookId);

            }
        }
        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
                //whereClause.append(" AND so.order_no LIKE '%").append(":filter").append("%'");
                whereClause.append(" AND so.order_no LIKE :filter");
                params.put("filter", filter);

            }
        }

        public void setState(String state) {
            if (StringUtils.isNotBlank(state)) {
                builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.valueOf(state)));
                whereClause.append(" AND so.order_status = :state");
                params.put("state", state);

            }
        }



        public void setStart(String start) {
            if (StringUtils.isNotBlank(start)) {
                whereClause.append(" AND so.outbound_date >= :start");
                params.put("start", start);
            }
        }

        public void setEnd(String end) {
            if (StringUtils.isNotBlank(end)) {
                whereClause.append(" AND so.outbound_date >= :start");
                params.put("end", end);
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
                whereClause.append(" AND so.customer_id = :customerId");
                params.put("customerId", customerId);
            }
        }

        //查询未退货订单
        public void setQueryUnReturnOrder(Integer queryUnReturnOrder) {
            if (queryUnReturnOrder == 1) {
                builder.and(qSalesOutbound.returnOrderId.isNull());
            }
        }

        public String getWhereClause() {
            return whereClause.toString();
        }

    }
}
