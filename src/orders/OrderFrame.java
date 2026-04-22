package orders;

import database.DBConnection;
import utils.ActivityLogStore;
import utils.ExcelExportUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class OrderFrame extends JPanel {

    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(140, 36);
    private static final Dimension SEARCH_FIELD_SIZE = new Dimension(220, 34);
    private static final Font CONTROL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    JTable table;
    DefaultTableModel model;
    private final List<Integer> orderIds = new ArrayList<>();
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;

    public OrderFrame(){

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JButton createBtn = new JButton("Create Order");
        JButton approveBtn = new JButton("Approve Order");
        JButton receiveBtn = new JButton("Receive Items");
        JButton deleteBtn = new JButton("Delete Order");
        JButton exportBtn = new JButton("Export");
        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(16);

        styleActionButton(createBtn);
        styleActionButton(approveBtn);
        styleActionButton(receiveBtn);
        styleActionButton(deleteBtn);
        styleActionButton(exportBtn);
        searchLabel.setFont(CONTROL_FONT);
        searchField.setPreferredSize(SEARCH_FIELD_SIZE);
        searchField.setFont(CONTROL_FONT);

        topPanel.add(createBtn);
        topPanel.add(approveBtn);
        topPanel.add(receiveBtn);
        topPanel.add(deleteBtn);
        topPanel.add(exportBtn);
        topPanel.add(searchLabel);
        topPanel.add(searchField);

        model = new DefaultTableModel();

        model.setColumnIdentifiers(new String[]{
                "ID","Supplier","Product","Quantity","Order Date","Status"
        });

        table = new JTable(model);
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setRowHeight(32);
        table.setFont(TABLE_FONT);
        table.getTableHeader().setFont(TABLE_HEADER_FONT);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(Color.WHITE);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(tableContainer, BorderLayout.CENTER);

        loadOrders();

        createBtn.addActionListener(e -> createOrder());
        approveBtn.addActionListener(e -> approveOrder());
        receiveBtn.addActionListener(e -> receiveItems());
        deleteBtn.addActionListener(e -> deleteOrder());
        exportBtn.addActionListener(e -> exportExcel());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                applySearchFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applySearchFilter();
            }
        });
    }

    void loadOrders(){

        model.setRowCount(0);
        orderIds.clear();

        try{

            Connection conn = DBConnection.getConnection();

            String sql = "SELECT * FROM purchase_orders ORDER BY id";

            PreparedStatement pst = conn.prepareStatement(sql);

            ResultSet rs = pst.executeQuery();

            while(rs.next()){

                int id = rs.getInt("id");
                String supplier = rs.getString("supplier");
                String product = rs.getString("product_name");
                int qty = rs.getInt("quantity");
                String date = rs.getString("order_date");
                String status = rs.getString("status");

                orderIds.add(id);
                model.addRow(new Object[]{orderIds.size(),supplier,product,qty,date,status});

            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void createOrder(){

        JComboBox<String> supplierBox = new JComboBox<>();
        JComboBox<String> productBox  = new JComboBox<>();
        JTextField quantity = new JTextField();

        // Populate supplier dropdown from suppliers table
        try{
            Connection conn = DBConnection.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT supplier_name FROM suppliers WHERE COALESCE(status,'Active')='Active' ORDER BY supplier_name");
            ResultSet rs = pst.executeQuery();
            while(rs.next()) supplierBox.addItem(rs.getString("supplier_name"));
            conn.close();
        }catch(Exception e){ e.printStackTrace(); }

        // Populate product dropdown from products table
        try{
            Connection conn = DBConnection.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT product_name FROM products ORDER BY product_name");
            ResultSet rs = pst.executeQuery();
            while(rs.next()) productBox.addItem(rs.getString("product_name"));
            conn.close();
        }catch(Exception e){ e.printStackTrace(); }

        if(supplierBox.getItemCount() == 0){
            JOptionPane.showMessageDialog(this,
                "No active suppliers found. Please add or activate a supplier first.");
            return;
        }

        if(productBox.getItemCount() == 0){
            JOptionPane.showMessageDialog(this,
                "No products found. Please add a product in Inventory first.");
            return;
        }

        Object[] fields = {
                "Supplier", supplierBox,
                "Product",  productBox,
                "Quantity", quantity
        };

        int option = JOptionPane.showConfirmDialog(null,fields,"Create Order",JOptionPane.OK_CANCEL_OPTION);

        if(option == JOptionPane.OK_OPTION){

            int confirm = JOptionPane.showOptionDialog(
                this,
                "Create this purchase order?\n" +
                "Supplier: " + supplierBox.getSelectedItem().toString() + "\n" +
                "Product: " + productBox.getSelectedItem().toString() + "\n" +
                "Quantity: " + quantity.getText().trim(),
                "Confirm Create Order",
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

                String sql = "INSERT INTO purchase_orders(supplier,product_name,quantity,order_date,status) VALUES(?,?,?,CURDATE(),'Pending')";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setString(1, supplierBox.getSelectedItem().toString());
                pst.setString(2, productBox.getSelectedItem().toString());
                pst.setInt(3, Integer.parseInt(quantity.getText().trim()));

                pst.executeUpdate();
                conn.close();

                loadOrders();
                ActivityLogStore.log(
                    "Created Purchase Order: " +
                    quantity.getText().trim() + " x " +
                    productBox.getSelectedItem().toString() +
                    " from " +
                    supplierBox.getSelectedItem().toString()
                );

            }catch(NumberFormatException nfe){
                JOptionPane.showMessageDialog(this,"Quantity must be a valid number.");
            }catch(Exception e){
                e.printStackTrace();
            }

        }

    }

    void approveOrder(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select an order first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);

        String currentStatus = model.getValueAt(modelRow,5).toString();

        if("Received".equalsIgnoreCase(currentStatus)){
            JOptionPane.showMessageDialog(this,"This order has already been received and cannot be modified.");
            return;
        }

        int id = orderIds.get(modelRow);

        int confirm = JOptionPane.showOptionDialog(
            this,
            "Approve this order?\n" +
            "Product: " + model.getValueAt(modelRow,2).toString() + "\n" +
            "Status: " + currentStatus,
            "Confirm Approve Order",
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

            String sql = "UPDATE purchase_orders SET status='Approved' WHERE id=? AND status<>'Received'";

            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setInt(1,id);

            int updatedRows = pst.executeUpdate();

            loadOrders();

            if(updatedRows > 0){
                ActivityLogStore.log("Approved Purchase Order for " + model.getValueAt(modelRow,2).toString());
            }

        }catch(Exception e){
            e.printStackTrace();
        }

    }

    void receiveItems(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select order first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);

        String currentStatus = model.getValueAt(modelRow,5).toString();

        if("Received".equalsIgnoreCase(currentStatus)){
            JOptionPane.showMessageDialog(this,"This order has already been received.");
            return;
        }

        int id = orderIds.get(modelRow);
        String product = model.getValueAt(modelRow,2).toString().trim();
        int qty = Integer.parseInt(model.getValueAt(modelRow,3).toString());

        int confirm = JOptionPane.showOptionDialog(
            this,
            "Receive items for this order?\n" +
            "Product: " + product + "\n" +
            "Quantity: " + qty,
            "Confirm Receive Items",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Yes", "No"},
            "No"
        );

        if(confirm != JOptionPane.YES_OPTION){
            return;
        }

        Connection conn = null;

        try{

            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Mark order as Received
            PreparedStatement pst = conn.prepareStatement(
                "UPDATE purchase_orders SET status='Received' WHERE id=? AND status<>'Received'");
            pst.setInt(1, id);
            int orderUpdated = pst.executeUpdate();
            pst.close();

            if(orderUpdated == 0){
                conn.rollback();
                JOptionPane.showMessageDialog(this,"This order has already been received.");
                loadOrders();
                return;
            }

            // 2. Add received quantity to matching product in inventory
            PreparedStatement pst2 = conn.prepareStatement(
                "UPDATE products SET quantity = quantity + ? WHERE product_name = ?");
            pst2.setInt(1, qty);
            pst2.setString(2, product);
            int inventoryUpdated = pst2.executeUpdate();
            pst2.close();

            if(inventoryUpdated == 0){
                // Product not found in inventory — rollback order status change
                conn.rollback();
                JOptionPane.showMessageDialog(this,
                    "No product named \"" + product + "\" found in Inventory.\n" +
                    "Add the product to Inventory first, then try again.");
                return;
            }

            conn.commit();

            loadOrders();
            ActivityLogStore.log("Received Items for Order: " + product + " (" + qty + ")");

            JOptionPane.showMessageDialog(this,"Order received — inventory quantity updated!");

        }catch(Exception e){

            if(conn != null){
                try{ conn.rollback(); }catch(Exception ex){ ex.printStackTrace(); }
            }
            e.printStackTrace();

        }finally{

            if(conn != null){
                try{ conn.setAutoCommit(true); conn.close(); }catch(Exception ex){ ex.printStackTrace(); }
            }

        }

    }

    void deleteOrder(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select an order first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = orderIds.get(modelRow);

        String supplier = model.getValueAt(modelRow,1).toString();
        String product = model.getValueAt(modelRow,2).toString();
        String quantity = model.getValueAt(modelRow,3).toString();
        String status = model.getValueAt(modelRow,5).toString();

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete this purchase order?\n" +
            "Supplier: " + supplier + "\n" +
            "Product: " + product + "\n" +
            "Quantity: " + quantity + "\n" +
            "Status: " + status,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if(confirm != JOptionPane.YES_OPTION){
            return;
        }

        try(
            Connection conn = DBConnection.getConnection();
            PreparedStatement pst = conn.prepareStatement("DELETE FROM purchase_orders WHERE id=?")
        ){
            pst.setInt(1, id);
            int deletedRows = pst.executeUpdate();

            if(deletedRows > 0){
                loadOrders();
                ActivityLogStore.log("Deleted Purchase Order: " + quantity + " x " + product + " from " + supplier);
                JOptionPane.showMessageDialog(this,"Purchase order deleted successfully.");
            }else{
                JOptionPane.showMessageDialog(this,"Order was not deleted. It may have already been removed.");
                loadOrders();
            }

        }catch(Exception e){
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,"Failed to delete purchase order.");
        }
    }

    void exportExcel(){
        ExcelExportUtil.exportTable(this, table, "purchase_orders.xlsx", "Orders");
        ActivityLogStore.log("Exported Purchase Orders Report");

    }

    private void applySearchFilter(){
        String text = searchField.getText().trim();
        if(text.isEmpty()){
            sorter.setRowFilter(null);
        }else{
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }

    public void refreshData(){
        loadOrders();
    }

    private void styleActionButton(JButton button){
        button.setPreferredSize(ACTION_BUTTON_SIZE);
        button.setFont(CONTROL_FONT);
        button.setFocusPainted(false);
    }

}