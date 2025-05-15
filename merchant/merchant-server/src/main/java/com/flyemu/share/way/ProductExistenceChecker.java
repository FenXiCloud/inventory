package com.flyemu.share.way;

import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.blazebit.persistence.querydsl.BlazeJPAQueryFactory;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrderItem;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseInboundItem;
import com.flyemu.share.entity.purchase.QPurchaseReturn;
import com.flyemu.share.entity.purchase.QPurchaseReturnItem;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOrderItem;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.entity.sales.QSalesReturn;
import com.flyemu.share.entity.sales.QSalesReturnItem;
import com.flyemu.share.service.AbsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductExistenceChecker extends AbsService {


    // QueryDSL 实体引用
    private static final QStockTake qStockTake = QStockTake.stockTake;
    private static final QStockTakeItem qStockTakeItem = QStockTakeItem.stockTakeItem;

    private static final QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private static final QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;

    private static final QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private static final QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;

    private static final QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private static final QPurchaseReturnItem qPurchaseReturnItem = QPurchaseReturnItem.purchaseReturnItem;

    private static final QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private static final QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private static final QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private static final QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;

    private static final QSalesReturn qSalesReturn = QSalesReturn.salesReturn;
    private static final QSalesReturnItem qSalesReturnItem = QSalesReturnItem.salesReturnItem;

    private static final QInventoryTransfer qInventoryTransfer = QInventoryTransfer.inventoryTransfer;
    private static final QInventoryTransferItem qInventoryTransferItem = QInventoryTransferItem.inventoryTransferItem;

    private static final QCostAdjustment qCostAdjustment = QCostAdjustment.costAdjustment;
    private static final QCostAdjustmentItem qCostAdjustmentItem = QCostAdjustmentItem.costAdjustmentItem;

    private static final QOtherInbound qOtherInbound = QOtherInbound.otherInbound;
    private static final QOtherInboundItem qOtherInboundItem = QOtherInboundItem.otherInboundItem;

    private static final QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;
    private static final QOtherOutboundItem qOtherOutboundItem = QOtherOutboundItem.otherOutboundItem;

    private static final QProduct qProduct = QProduct.product;

    public boolean checkOutTheProduct(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<Product> query = bqf.selectFrom(qProduct);

        if (type != null && type == 5) {
            query.where(qProduct.unitId.eq(productId));
        }
        Long result = query.select(qProduct.id).fetchFirst();
        return result != null && result > 0;
    }

    public boolean existsInStockTake(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<StockTake> query = bqf.selectFrom(qStockTake)
                .join(qStockTakeItem).on(qStockTakeItem.StockTakeId.eq(qStockTake.id));

        if (type != null && type == 1) {
            query.where(qStockTakeItem.productId.eq(productId));
        } else if (type == 4) {
            query.where(qStockTakeItem.warehouseId.eq(productId));
        }

        Long result = query.select(qStockTake.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInPurchaseOrder(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.purchase.PurchaseOrder> query = bqf.selectFrom(qPurchaseOrder)
                .join(qPurchaseOrderItem).on(qPurchaseOrderItem.purchaseOrderId.eq(qPurchaseOrder.id));

        if (type != null && type == 1) {
            query.where(qPurchaseOrderItem.productId.eq(productId));
        } else if (type == 3) {
            query.where(qPurchaseOrder.supplierId.eq(productId));
        } else if (type == 4) {
            query.where(qPurchaseOrderItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qPurchaseOrderItem.baseUnitId.eq(productId));
        }


        Long result = query.select(qPurchaseOrder.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInPurchaseInbound(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.purchase.PurchaseInbound> query = bqf.selectFrom(qPurchaseInbound)
                .join(qPurchaseInboundItem).on(qPurchaseInboundItem.purchaseInboundId.eq(qPurchaseInbound.id));

        if (type != null && type == 1) {
            query.where(qPurchaseInboundItem.productId.eq(productId));
        } else if (type == 3) {
            query.where(qPurchaseInbound.supplierId.eq(productId));
        } else if (type == 4) {
            query.where(qPurchaseInboundItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qPurchaseInboundItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qPurchaseInbound.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInPurchaseReturn(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.purchase.PurchaseReturn> query = bqf.selectFrom(qPurchaseReturn)
                .join(qPurchaseReturnItem).on(qPurchaseReturnItem.purchaseReturnId.eq(qPurchaseReturn.id));

        if (type != null && type == 1) {
            query.where(qPurchaseReturnItem.productId.eq(productId));
        } else if (type == 3) {
            query.where(qPurchaseReturn.supplierId.eq(productId));
        } else if (type == 4) {
            query.where(qPurchaseReturnItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qPurchaseReturnItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qPurchaseReturn.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInSalesOrder(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.sales.SalesOrder> query = bqf.selectFrom(qSalesOrder)
                .join(qSalesOrderItem).on(qSalesOrderItem.salesOrderId.eq(qSalesOrder.id));

        if (type != null && type == 1) {
            query.where(qSalesOrderItem.productId.eq(productId));
        } else if (type == 2) {
            query.where(qSalesOrder.customerId.eq(productId));
        } else if (type == 4) {
            query.where(qSalesOrderItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qSalesOrderItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qSalesOrder.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInSalesOutbound(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.sales.SalesOutbound> query = bqf.selectFrom(qSalesOutbound)
                .join(qSalesOutboundItem).on(qSalesOutboundItem.salesOutboundId.eq(qSalesOutbound.id));

        if (type != null && type == 1) {
            query.where(qSalesOutboundItem.productId.eq(productId));
        } else if (type == 2) {
            query.where(qSalesOutbound.customerId.eq(productId));
        } else if (type == 4) {
            query.where(qSalesOutboundItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qSalesOutboundItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qSalesOutbound.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInSalesReturn(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<com.flyemu.share.entity.sales.SalesReturn> query = bqf.selectFrom(qSalesReturn)
                .join(qSalesReturnItem).on(qSalesReturnItem.salesReturnId.eq(qSalesReturn.id));

        if (type != null && type == 1) {
            query.where(qSalesReturnItem.productId.eq(productId));
        } else if (type == 2) {
            query.where(qSalesReturn.customerId.eq(productId));
        } else if (type == 4) {
            query.where(qSalesReturnItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qSalesReturnItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qSalesReturn.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInInventoryTransfer(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<InventoryTransfer> query = bqf.selectFrom(qInventoryTransfer)
                .join(qInventoryTransferItem).on(qInventoryTransferItem.inventoryTransferId.eq(qInventoryTransfer.id));

        if (type != null && type == 1) {
            query.where(qInventoryTransferItem.productId.eq(productId));
        } else if (type == 4) {
            query.where(
                    qInventoryTransfer.FromWarehouseId.eq(productId)
                            .or(qInventoryTransfer.ToWarehouseId.eq(productId)));
        }
        Long result = query.select(qInventoryTransfer.id).fetchFirst();
        return result != null && result > 0;
    }


    public boolean existsInCostAdjustment(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<CostAdjustment> query = bqf.selectFrom(qCostAdjustment)
                .join(qCostAdjustmentItem).on(qCostAdjustmentItem.costAdjustmentId.eq(qCostAdjustment.id));

        if (type != null && type == 1) {
            query.where(qCostAdjustmentItem.productId.eq(productId));
        } else if (type == 4) {
            query.where(qCostAdjustmentItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qCostAdjustmentItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qCostAdjustment.id).fetchFirst();

        return result != null && result > 0;
    }


    public boolean existsInOtherInbound(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<OtherInbound> query = bqf.selectFrom(qOtherInbound)
                .join(qOtherInboundItem).on(qOtherInboundItem.otherInboundId.eq(qOtherInbound.id));

        if (type != null && type == 1) {
            query.where(qOtherInboundItem.productId.eq(productId));
        } else if (type == 2) {
            query.where(qOtherInbound.customerId.eq(productId));
        } else if (type == 3) {
            query.where(qOtherInbound.supplierId.eq(productId));
        } else if (type == 4) {
            query.where(qOtherInboundItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qOtherInboundItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qOtherInbound.id).fetchFirst();

        return result != null && result > 0;
    }

    public boolean existsInOtherOutbound(Long productId, Integer type) {
        if (productId == null || productId <= 0) return false;

        BlazeJPAQuery<OtherOutbound> query = bqf.selectFrom(qOtherOutbound)
                .join(qOtherOutboundItem).on(qOtherOutboundItem.otherOutboundId.eq(qOtherOutbound.id));

        if (type != null && type == 1) {
            query.where(qOtherOutboundItem.productId.eq(productId));
        } else if (type == 2) {
            query.where(qOtherOutbound.customerId.eq(productId));
        } else if (type == 4) {
            query.where(qOtherOutboundItem.warehouseId.eq(productId));
        } else if (type == 5) {
            query.where(qOtherOutboundItem.baseUnitId.eq(productId));
        }

        Long result = query.select(qOtherOutbound.id).fetchFirst();

        return result != null && result > 0;
    }
}
