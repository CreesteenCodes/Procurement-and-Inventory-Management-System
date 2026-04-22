package supplier;

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

public class SupplierFrame extends JPanel {

    private static final Dimension ACTION_BUTTON_SIZE = new Dimension(140, 36);
    private static final Dimension SEARCH_FIELD_SIZE = new Dimension(220, 34);
    private static final Font CONTROL_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font TABLE_HEADER_FONT = new Font("Segoe UI", Font.BOLD, 14);

    JTable table;
    DefaultTableModel model;
    private final Runnable onSuppliersChanged;
    private final List<Integer> supplierIds = new ArrayList<>();
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JCheckBox activeOnlyCheck;

    public SupplierFrame(){
        this(null);
    }

    public SupplierFrame(Runnable onSuppliersChanged){

        this.onSuppliersChanged = onSuppliersChanged;

        setLayout(new BorderLayout());

        ensureStatusColumnExists();

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JButton addBtn = new JButton("Add Supplier");
        JButton editBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton exportBtn = new JButton("Export");
        JLabel searchLabel = new JLabel("Search:");
        searchField = new JTextField(16);
        activeOnlyCheck = new JCheckBox("Active Only");

        styleActionButton(addBtn);
        styleActionButton(editBtn);
        styleActionButton(deleteBtn);
        styleActionButton(exportBtn);
        searchLabel.setFont(CONTROL_FONT);
        searchField.setPreferredSize(SEARCH_FIELD_SIZE);
        searchField.setFont(CONTROL_FONT);
        activeOnlyCheck.setFont(CONTROL_FONT);

        topPanel.add(addBtn);
        topPanel.add(editBtn);
        topPanel.add(deleteBtn);
        topPanel.add(exportBtn);
        topPanel.add(searchLabel);
        topPanel.add(searchField);
        topPanel.add(activeOnlyCheck);

        model = new DefaultTableModel();

        model.setColumnIdentifiers(new String[]{
            "ID","Supplier Name","Contact Person","Phone","Email","Address","Status"
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

        loadSuppliers();

        addBtn.addActionListener(e -> addSupplier());
        editBtn.addActionListener(e -> editSupplier());
        deleteBtn.addActionListener(e -> deleteSupplier());
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
        activeOnlyCheck.addActionListener(e -> applySearchFilter());
    }

    void ensureStatusColumnExists(){

        try(
                Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement()
        ){
            st.executeUpdate("ALTER TABLE suppliers ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'Active'");
        }catch(SQLException e){
            // Duplicate column error means status already exists.
            if(e.getErrorCode() != 1060){
                e.printStackTrace();
            }
        }
    }

    void loadSuppliers(){

        model.setRowCount(0);
        supplierIds.clear();

        try{

            Connection conn = DBConnection.getConnection();

            String sql = "SELECT id,supplier_name,contact_person,phone,email,address,COALESCE(status,'Active') AS status FROM suppliers ORDER BY id";

            PreparedStatement pst = conn.prepareStatement(sql);

            ResultSet rs = pst.executeQuery();

            while(rs.next()){

                int id = rs.getInt("id");
                String name = rs.getString("supplier_name");
                String contact = rs.getString("contact_person");
                String phone = rs.getString("phone");
                String email = rs.getString("email");
                String address = rs.getString("address");
                String status = rs.getString("status");

                supplierIds.add(id);
                model.addRow(new Object[]{supplierIds.size(),name,contact,phone,email,address,status});
            }

        }catch(Exception e){
            e.printStackTrace();
        }
    }

    void addSupplier(){

        JTextField name = new JTextField();
        JTextField contact = new JTextField();
        JTextField phone = new JTextField();
        JTextField email = new JTextField();
        JTextField address = new JTextField();
        JComboBox<String> status = new JComboBox<>(new String[]{"Active","Inactive"});

        Object[] fields = {
                "Supplier Name",name,
                "Contact Person",contact,
                "Phone",phone,
                "Email",email,
            "Address",address,
            "Status",status
        };

        int option = JOptionPane.showConfirmDialog(null,fields,"Add Supplier",JOptionPane.OK_CANCEL_OPTION);

        if(option == JOptionPane.OK_OPTION){

            int confirm = JOptionPane.showOptionDialog(
                this,
                "Add this supplier?\n" +
                "Supplier: " + name.getText().trim(),
                "Confirm Add Supplier",
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

                String sql = "INSERT INTO suppliers(supplier_name,contact_person,phone,email,address,status) VALUES(?,?,?,?,?,?)";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setString(1,name.getText());
                pst.setString(2,contact.getText());
                pst.setString(3,phone.getText());
                pst.setString(4,email.getText());
                pst.setString(5,address.getText());
                pst.setString(6,status.getSelectedItem().toString());

                pst.executeUpdate();

                loadSuppliers();
                notifySuppliersChanged();
                ActivityLogStore.log("Added Supplier: " + name.getText().trim());

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    void editSupplier(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select supplier first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = supplierIds.get(modelRow);

        JTextField name = new JTextField(model.getValueAt(modelRow,1).toString());
        JTextField contact = new JTextField(model.getValueAt(modelRow,2).toString());
        JTextField phone = new JTextField(model.getValueAt(modelRow,3).toString());
        JTextField email = new JTextField(model.getValueAt(modelRow,4).toString());
        JTextField address = new JTextField(model.getValueAt(modelRow,5).toString());
        JComboBox<String> status = new JComboBox<>(new String[]{"Active","Inactive"});
        status.setSelectedItem(model.getValueAt(modelRow,6).toString());

        Object[] fields = {
                "Supplier Name",name,
                "Contact Person",contact,
                "Phone",phone,
                "Email",email,
            "Address",address,
            "Status",status
        };

        int option = JOptionPane.showConfirmDialog(null,fields,"Edit Supplier",JOptionPane.OK_CANCEL_OPTION);

        if(option == JOptionPane.OK_OPTION){

            int confirm = JOptionPane.showOptionDialog(
                this,
                "Update this supplier?\n" +
                "Supplier: " + name.getText().trim(),
                "Confirm Update Supplier",
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

                String sql = "UPDATE suppliers SET supplier_name=?,contact_person=?,phone=?,email=?,address=?,status=? WHERE id=?";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setString(1,name.getText());
                pst.setString(2,contact.getText());
                pst.setString(3,phone.getText());
                pst.setString(4,email.getText());
                pst.setString(5,address.getText());
                pst.setString(6,status.getSelectedItem().toString());
                pst.setInt(7,id);

                pst.executeUpdate();

                loadSuppliers();
                notifySuppliersChanged();
                ActivityLogStore.log("Updated Supplier: " + name.getText().trim());

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    void deleteSupplier(){

        int row = table.getSelectedRow();

        if(row < 0){
            JOptionPane.showMessageDialog(this,"Select supplier first");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        int id = supplierIds.get(modelRow);
        String supplierName = model.getValueAt(modelRow,1).toString();

        int confirm = JOptionPane.showOptionDialog(
            this,
            "Delete this supplier?\n" +
            "Supplier: " + supplierName,
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[]{"Yes", "No"},
            "No"
        );

        if(confirm == JOptionPane.YES_OPTION){

            try{

                Connection conn = DBConnection.getConnection();

                String sql = "DELETE FROM suppliers WHERE id=?";

                PreparedStatement pst = conn.prepareStatement(sql);

                pst.setInt(1,id);

                pst.executeUpdate();

                loadSuppliers();
                notifySuppliersChanged();
                ActivityLogStore.log("Deleted Supplier: " + supplierName);

            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    void exportExcel(){
        ExcelExportUtil.exportTable(this, table, "suppliers.xlsx", "Suppliers");
        ActivityLogStore.log("Exported Suppliers Report");

    }

    private void notifySuppliersChanged(){
        if(onSuppliersChanged != null){
            onSuppliersChanged.run();
        }
    }

    private void applySearchFilter(){

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        String text = searchField.getText().trim();

        if(!text.isEmpty()){
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }

        if(activeOnlyCheck.isSelected()){
            filters.add(new RowFilter<Object, Object>() {
                @Override
                public boolean include(Entry<? extends Object, ? extends Object> entry) {
                    String status = entry.getStringValue(6);
                    return "Active".equalsIgnoreCase(status == null ? "" : status.trim());
                }
            });
        }

        if(filters.isEmpty()){
            sorter.setRowFilter(null);
        }else{
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }
    }

    public void refreshData(){
        loadSuppliers();
    }

    private void styleActionButton(JButton button){
        button.setPreferredSize(ACTION_BUTTON_SIZE);
        button.setFont(CONTROL_FONT);
        button.setFocusPainted(false);
    }

}