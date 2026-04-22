package models;

public class Product {

    private int id;
    private String productName;
    private String category;
    private int quantity;
    private double price;
    private String supplier;
    private int reorderLevel;

    public Product(int id, String productName, String category, int quantity, double price, String supplier, int reorderLevel) {
        this.id = id;
        this.productName = productName;
        this.category = category;
        this.quantity = quantity;
        this.price = price;
        this.supplier = supplier;
        this.reorderLevel = reorderLevel;
    }

    public int getId() { return id; }
    public String getProductName() { return productName; }
    public String getCategory() { return category; }
    public int getQuantity() { return quantity; }
    public double getPrice() { return price; }
    public String getSupplier() { return supplier; }
    public int getReorderLevel() { return reorderLevel; }

}