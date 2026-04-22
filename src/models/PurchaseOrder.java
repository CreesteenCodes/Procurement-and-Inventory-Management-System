package models;

public class PurchaseOrder {

    private int id;
    private String supplier;
    private String product;
    private int quantity;
    private String orderDate;
    private String status;

    public PurchaseOrder(int id,String supplier,String product,int quantity,String orderDate,String status){

        this.id = id;
        this.supplier = supplier;
        this.product = product;
        this.quantity = quantity;
        this.orderDate = orderDate;
        this.status = status;

    }

    public int getId(){ return id; }
    public String getSupplier(){ return supplier; }
    public String getProduct(){ return product; }
    public int getQuantity(){ return quantity; }
    public String getOrderDate(){ return orderDate; }
    public String getStatus(){ return status; }

}