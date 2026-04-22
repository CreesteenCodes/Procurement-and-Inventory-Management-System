package inventory;

import database.DBConnection;
import utils.ActivityLogStore;
import utils.ExcelExportUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class InventoryFrame extends JPanel {

    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(140, 36);
    private static final Dimension SEARCH_FIELD_SIZE = new Dimension(220, 34);
    private static final Font CONTROL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");

    private static final String[] PRODUCT_CATEGORIES = {
        "Tops",
        "Bottoms",
        "Dresses",
        "Outerwear",
        "Activewear",
        "Sportswear",
        "Underwear",
        "Innerwear",
        "Sleepwear",
        "Footwear",
        "Accessories"
    };

    JTable table;
    DefaultTableModel model;
    private final Runnable onProductsChanged;
    private final List<Integer> productIds = new ArrayList<>();
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JCheckBox lowStockOnlyCheck;

    public InventoryFrame(){
        this(null);
    }

    public InventoryFrame(Runnable onProductsChanged){

        this.onProductsChanged = onProductsChanged;

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JButton addBtn = new JButton("Add Product");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton exportBtn = new JButton("Export");
        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(14);
        lowStockOnlyCheck = new JCheckBox("Low Stock Only");

        styleActionButton(addBtn);
        styleActionButton(updateBtn);
        styleActionButton(deleteBtn);
        styleActionButton(exportBtn);
        searchLabel.setFont(CONTROL_FONT);
        searchField.setPreferredSize(SEARCH_FIELD_SIZE);
        searchField.setFont(CONTROL_FONT);
        lowStockOnlyCheck.setFont(CONTROL_FONT);

        topPanel.add(addBtn);
        topPanel.add(updateBtn);
        topPanel.add(deleteBtn);
        topPanel.add(exportBtn);
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        topPanel.add(lowStockOnlyCheck);

        model = new DefaultTableModel();

        model.setColumnIdentifiers(new String[]{
                "ID","Product Name","Category","Quantity","Price","Supplier","Reorder Level"
        });

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setFont(TABLE_FONT);
        table.getTableHeader().setFont(TABLE_HEADER_FONT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            {
                setHorizontalAlignment(SwingConstants.RIGHT);
            }

            @Override
            protected void setValue(Object value) {
                if(value instanceof Number) {
                    setText(PRICE_FORMAT.format(((Number) value).doubleValue()));
                    return;
                }

                if(value == null) {
                    setText("");
                    return;
                }

                try {
                    setText(PRICE_FORMAT.format(Double.parseDouble(value.toString().replace(",", "").trim())));
                } catch(Exception ignored) {
                    setText(value.toString());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);

        loadProducts();

        addBtn.addActionListener(e -> addProduct());
        updateBtn.addActionListener(e -> updateProduct());
        deleteBtn.addActionListener(e -> deleteProduct());
        exportBtn.addActionListener(e -> exportToExcel());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applyInventoryFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applyInventoryFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyInventoryFilters();
            }
        });
        lowStockOnlyCheck.addActionListener(e -> applyInventoryFilters());
    }

    void loadProducts(){

        model.setRowCount(0);
        productIds.clear();

        try{

            Connection conn = DBConnection.getConnection();

            String sql = "SELECT * FROM products ORDER BY id";

            PreparedStatement pst = conn.prepareStatement(sql);

            ResultSet rs = pst.executeQuery();

            while(rs.next()){

                int id = rs.getInt("id");
                String name = rs.getString("product_name");
                String cat = rs.getString("category");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                String sup = rs.getString("supplier");
                int reorder = rs.getInt("reorder_level");

                productIds.add(id);
                model.addRow(new Object[]{productIds.size(),name,cat,qty,price,sup,reorder});

            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void addProduct(){

        JTextField name = new JTextField();
        JComboBox<String> category = createCategoryComboBox(null);
        JTextField quantity = new JTextField();
        JTextField price = new JTextField();
        JComboBox<String> supplier = createSupplierComboBox(null);
        JTextField reorder = new JTextField();

        if(supplier.getItemCount() == 0){
            JOptionPane.showMessageDialog(this,"No active suppliers found. Please add or activate a supplier first.");
            return;
        }

        Object[] fields = {
                "Product Name", name,
                "Category", category,
                "Quantity", quantity,
                "Price", price,
                "Supplier", supplier,
                "Reorder Level", reorder
        };

        int option = JOptionPane.showConfirmDialog(null,fields,"Add Product",JOptionPane.OK_CANCEL_OPTION);

        if(option == JOptionPane.OK_OPTION){

            int confirm = JOptionPane.showOptionDialog(
                this,
                "Add this product?\n" +
                "Product: " + name.getText().trim(),
                "Confirm Add Product",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new Object[]{"Yes", "No"},
                "No"
            );

            if(confirm != JOptionPane.YES_OPTION){
                return;
            }

            try{

                Connection conn = DBConnection.getConnection();

                String sql = "INSERT INTO products(product_name,category,quantity,price,supplier,reorder_level) VALUES(?,?,?,?,?,?)";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setString(1,name.getText());
                pst.setString(2,category.getSelectedItem().toString());
                pst.setInt(3,Integer.parseInt(quantity.getText()));
                pst.setDouble(4,parsePriceInput(price.getText()));
                pst.setString(5,supplier.getSelectedItem().toString());
                pst.setInt(6,Integer.parseInt(reorder.getText()));

                pst.executeUpdate();

                loadProducts();
                notifyProductsChanged();
                ActivityLogStore.log("Added Product: " + name.getText().trim());

            }catch(NumberFormatException e){
                JOptionPane.showMessageDialog(this,"Quantity, Price, and Reorder Level must be valid numbers.");
            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }

    void updateProduct(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select product first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = productIds.get(modelRow);

        JTextField name = new JTextField(model.getValueAt(modelRow,1).toString());
        JComboBox<String> category = createCategoryComboBox(model.getValueAt(modelRow,2).toString());
        JTextField quantity = new JTextField(model.getValueAt(modelRow,3).toString());
        JTextField price = new JTextField(formatPriceInput(model.getValueAt(modelRow,4)));
        JComboBox<String> supplier = createSupplierComboBox(model.getValueAt(modelRow,5).toString());
        JTextField reorder = new JTextField(model.getValueAt(modelRow,6).toString());

        if(supplier.getItemCount() == 0){
            JOptionPane.showMessageDialog(this,"No active suppliers found. Please activate a supplier before updating this product.");
            return;
        }

        Object[] fields = {
                "Product Name", name,
                "Category", category,
                "Quantity", quantity,
                "Price", price,
                "Supplier", supplier,
                "Reorder Level", reorder
        };

        int option = JOptionPane.showConfirmDialog(this, fields, "Update Product", JOptionPane.OK_CANCEL_OPTION);

        if(option != JOptionPane.OK_OPTION){
            return;
        }

        int confirm = JOptionPane.showOptionDialog(
            this,
            "Update this product?\n" +
            "Product: " + name.getText().trim(),
            "Confirm Update Product",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Yes", "No"},
            "No"
        );

        if(confirm != JOptionPane.YES_OPTION){
            return;
        }

        try{

            Connection conn = DBConnection.getConnection();

            String sql = "UPDATE products SET product_name=?,category=?,quantity=?,price=?,supplier=?,reorder_level=? WHERE id=?";

            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1,name.getText().trim());
            pst.setString(2,category.getSelectedItem().toString());
            pst.setInt(3,Integer.parseInt(quantity.getText().trim()));
            pst.setDouble(4,parsePriceInput(price.getText()));
            pst.setString(5,supplier.getSelectedItem().toString());
            pst.setInt(6,Integer.parseInt(reorder.getText().trim()));
            pst.setInt(7,id);

            pst.executeUpdate();

            loadProducts();
            notifyProductsChanged();
            ActivityLogStore.log("Updated Product: " + name.getText().trim());

        }catch(NumberFormatException e){
            JOptionPane.showMessageDialog(this,"Quantity, Price, and Reorder Level must be valid numbers.");
        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void deleteProduct(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select product first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = productIds.get(modelRow);
        String productName = model.getValueAt(modelRow,1).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this product?\n" +
            "Product: " + productName,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if(confirm != JOptionPane.YES_OPTION){
            return;
        }

        try{

            Connection conn = DBConnection.getConnection();

            String sql = "DELETE FROM products WHERE id=?";

            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setInt(1,id);

            pst.executeUpdate();

            loadProducts();
            notifyProductsChanged();
            ActivityLogStore.log("Deleted Product: " + productName);

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void exportToExcel(){
        ExcelExportUtil.exportTable(this, table, "inventory.xlsx", "Inventory");
        ActivityLogStore.log("Exported Inventory Report");

    }

    private void notifyProductsChanged(){
        if(onProductsChanged != null){
            onProductsChanged.run();
        }
    }

    private void applyInventoryFilters(){

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String text = searchField.getText().trim();

        if(!text.isEmpty()){
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }

        if(lowStockOnlyCheck.isSelected()){
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    try{
                        int qty = Integer.parseInt(entry.getStringValue(3));
                        int reorder = Integer.parseInt(entry.getStringValue(6));
                        return qty <= reorder;
                    }catch(Exception e){
                        return false;
                    }
                }
            });
        }

        if(filters.isEmpty()){
            sorter.setRowFilter(null);
        }else{
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    private JComboBox<String> createSupplierComboBox(String selectedSupplier){

        JComboBox<String> supplierBox = new JComboBox<>();

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement(
                    "SELECT supplier_name FROM suppliers WHERE COALESCE(status,'Active')='Active' ORDER BY supplier_name");
                ResultSet rs = pst.executeQuery()
        ){
            while(rs.next()){
                supplierBox.addItem(rs.getString("supplier_name"));
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        if(selectedSupplier != null && !selectedSupplier.trim().isEmpty()){
            boolean found = false;

            for(int i = 0; i < supplierBox.getItemCount(); i++){
                if(selectedSupplier.equals(supplierBox.getItemAt(i))){
                    found = true;
                    break;
                }
            }

            if(found){
                supplierBox.setSelectedItem(selectedSupplier);
            }
        }

        return supplierBox;
    }

    private JComboBox<String> createCategoryComboBox(String selectedCategory){

        JComboBox<String> categoryBox = new JComboBox<>(PRODUCT_CATEGORIES);

        if(selectedCategory != null && !selectedCategory.trim().isEmpty()){
            boolean found = false;

            for(int i = 0; i < categoryBox.getItemCount(); i++){
                if(selectedCategory.equals(categoryBox.getItemAt(i))){
                    found = true;
                    break;
                }
            }

            if(!found){
                categoryBox.addItem(selectedCategory);
            }

            categoryBox.setSelectedItem(selectedCategory);
        }

        return categoryBox;
    }

    public void refreshData(){
        loadProducts();
    }

    private double parsePriceInput(String rawPrice){
        if(rawPrice == null){
            throw new NumberFormatException("Price is empty");
        }

        String normalized = rawPrice.replace(",", "").trim();
        return Double.parseDouble(normalized);
    }

    private String formatPriceInput(Object value){
        if(value instanceof Number){
            return PRICE_FORMAT.format(((Number) value).doubleValue());
        }

        if(value == null){
            return "";
        }

        try{
            return PRICE_FORMAT.format(Double.parseDouble(value.toString().replace(",", "").trim()));
        }catch(Exception ignored){
            return value.toString();
        }
    }

    private void styleActionButton(JButton button){
        button.setPreferredSize(ACTION_BUTTON_SIZE);
        button.setFont(CONTROL_FONT);
        button.setFocusPainted(false);
    }

}