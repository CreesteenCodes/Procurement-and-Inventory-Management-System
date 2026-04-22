package dashboard;

import auth.LoginFrame;
import database.DBConnection;
import inventory.InventoryFrame;
import models.User;
import orders.OrderFrame;
import supplier.SupplierFrame;
import utils.ActivityLogStore;
import utils.ActivityLogStore.ActivityEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;

public class DashboardFrame extends JFrame {

    private static final DateTimeFormatter ACTIVITY_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
    private static final DateTimeFormatter CLOCK_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy   hh:mm:ss a");

    public DashboardFrame(User user){

        setTitle("Céleste Retail Management System");
        setSize(1100,650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // TOP MENU BAR
        JPanel topMenuBar = new JPanel(new BorderLayout(15,0));
        topMenuBar.setBackground(new Color(10,18,40));
        topMenuBar.setBorder(BorderFactory.createEmptyBorder(10,15,10,15));
        topMenuBar.setPreferredSize(new Dimension(0,86));

        JLabel brandLabel = new JLabel("Céleste Couture", JLabel.LEFT);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setFont(new Font("Serif", Font.BOLD, 27));
        brandLabel.setVerticalAlignment(SwingConstants.CENTER);

        JButton dashboardBtn = new JButton("Dashboard");
        JButton inventoryBtn = new JButton("Inventory");
        JButton supplierBtn = new JButton("Suppliers");
        JButton purchaseBtn = new JButton("Purchase Orders");
        JButton logoutBtn = new JButton("Logout");

        makeMenuButtonPlain(dashboardBtn);
        makeMenuButtonPlain(inventoryBtn);
        makeMenuButtonPlain(supplierBtn);
        makeMenuButtonPlain(purchaseBtn);
        makeMenuButtonPlain(logoutBtn);

        JPanel menuButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        menuButtonsPanel.setOpaque(false);
        menuButtonsPanel.add(dashboardBtn);
        menuButtonsPanel.add(inventoryBtn);
        menuButtonsPanel.add(supplierBtn);
        menuButtonsPanel.add(purchaseBtn);
        menuButtonsPanel.add(logoutBtn);

        JPanel rightButtonsContainer = new JPanel(new GridBagLayout());
        rightButtonsContainer.setOpaque(false);
        GridBagConstraints rightButtonsGbc = new GridBagConstraints();
        rightButtonsGbc.gridx = 0;
        rightButtonsGbc.gridy = 0;
        rightButtonsGbc.weightx = 1.0;
        rightButtonsGbc.anchor = GridBagConstraints.EAST;
        rightButtonsContainer.add(menuButtonsPanel, rightButtonsGbc);

        topMenuBar.add(brandLabel, BorderLayout.WEST);
        topMenuBar.add(rightButtonsContainer, BorderLayout.CENTER);

        Color sectionHeaderColor = new Color(22, 52, 80);

        // Dashboard overview header
        JLabel overviewTitle = new JLabel("Dashboard Overview");
        overviewTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        overviewTitle.setForeground(sectionHeaderColor);

        JLabel clockLabel = new JLabel(LocalDateTime.now().format(CLOCK_FORMAT));
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clockLabel.setForeground(new Color(45, 70, 92));
        clockLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel overviewHeaderPanel = new JPanel(new BorderLayout());
        overviewHeaderPanel.setOpaque(false);
        overviewHeaderPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        overviewHeaderPanel.add(overviewTitle, BorderLayout.WEST);
        overviewHeaderPanel.add(clockLabel, BorderLayout.EAST);

        // Dashboard scorecards row
        JPanel dashboardCardsPanel = new JPanel(new GridLayout(1,4,16,0));
        dashboardCardsPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 8, 0));

        JLabel totalProductsValue = new JLabel("0");
        JLabel lowStockValue = new JLabel("0");
        JLabel activeSuppliersValue = new JLabel("0");
        JLabel recentOrdersValue = new JLabel("0");

        dashboardCardsPanel.add(createCard("Total Products", totalProductsValue));
        dashboardCardsPanel.add(createCard("Low Stock Items", lowStockValue));
        dashboardCardsPanel.add(createCard("Active Suppliers", activeSuppliersValue));
        dashboardCardsPanel.add(createCard("Recent Orders", recentOrdersValue));

        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        JPanel dashboardTopSection = new JPanel(new BorderLayout());
        dashboardTopSection.setOpaque(false);
        dashboardTopSection.add(overviewHeaderPanel, BorderLayout.NORTH);
        dashboardTopSection.add(dashboardCardsPanel, BorderLayout.CENTER);
        dashboardPanel.add(dashboardTopSection, BorderLayout.NORTH);

        DefaultListModel<String> activityListModel = new DefaultListModel<>();
        JList<String> activityList = new JList<>(activityListModel);
        activityList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        activityList.setBackground(UIManager.getColor("Table.background") != null ? UIManager.getColor("Table.background") : Color.WHITE);
        activityList.setFixedCellHeight(28);

        JScrollPane activityScrollPane = new JScrollPane(activityList);
        activityScrollPane.getViewport().setBackground(UIManager.getColor("Table.background") != null ? UIManager.getColor("Table.background") : Color.WHITE);
        activityScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel activityTitle = new JLabel("Recent Activities");
        activityTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        activityTitle.setForeground(sectionHeaderColor);

        JComboBox<String> activityFilter = new JComboBox<>(new String[]{"Day", "Week", "Month"});
        activityFilter.setSelectedItem("Day");
        styleActivityFilter(activityFilter);

        JPanel activityHeaderPanel = new JPanel(new BorderLayout());
        activityHeaderPanel.setOpaque(false);
        activityHeaderPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        activityHeaderPanel.add(activityTitle, BorderLayout.WEST);
        activityHeaderPanel.add(activityFilter, BorderLayout.EAST);

        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setOpaque(false);
        activityPanel.add(activityHeaderPanel, BorderLayout.NORTH);
        activityPanel.add(activityScrollPane, BorderLayout.CENTER);

        dashboardPanel.add(activityPanel, BorderLayout.CENTER);

        ensureSupplierStatusColumnExists();

        Runnable refreshDashboardStats = () -> {
            updateTotalProducts(totalProductsValue);
            updateLowStockItems(lowStockValue);
            updateActiveSuppliers(activeSuppliersValue);
            updateRecentOrders(recentOrdersValue);
        };

        refreshDashboardStats.run();

        Runnable refreshActivitySection = () -> refreshActivityList(activityListModel, (String) activityFilter.getSelectedItem());
        Runnable activityListener = () -> SwingUtilities.invokeLater(refreshActivitySection);
        ActivityLogStore.addListener(activityListener);
        refreshActivitySection.run();

        if(user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()){
            ActivityLogStore.log("User Logged In: " + user.getUsername().trim());
        }else{
            ActivityLogStore.log("User Logged In");
        }

        CardLayout contentLayout = new CardLayout();
        JPanel contentPanel = new JPanel(contentLayout);

        InventoryFrame inventoryFrame = new InventoryFrame(refreshDashboardStats);
        SupplierFrame supplierFrame = new SupplierFrame(refreshDashboardStats);
        OrderFrame orderFrame = new OrderFrame();

        contentPanel.add(dashboardPanel, "dashboard");
        contentPanel.add(inventoryFrame, "inventory");
        contentPanel.add(supplierFrame, "supplier");
        contentPanel.add(orderFrame, "orders");

        add(topMenuBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        Timer liveRefreshTimer = new Timer(3000, e -> {
            refreshDashboardStats.run();
            if(inventoryFrame.isShowing()) inventoryFrame.refreshData();
            if(supplierFrame.isShowing()) supplierFrame.refreshData();
            if(orderFrame.isShowing()) orderFrame.refreshData();
        });
        liveRefreshTimer.start();

        Timer clockTimer = new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FORMAT)));
        clockTimer.start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                liveRefreshTimer.stop();
                clockTimer.stop();
                ActivityLogStore.removeListener(activityListener);
            }
        });

        activityFilter.addActionListener(e -> refreshActivitySection.run());

        dashboardBtn.addActionListener(e -> {
            refreshDashboardStats.run();
            contentLayout.show(contentPanel, "dashboard");
        });

        inventoryBtn.addActionListener(e -> {
            inventoryFrame.refreshData();
            contentLayout.show(contentPanel, "inventory");
        });

        supplierBtn.addActionListener(e -> {
            supplierFrame.refreshData();
            contentLayout.show(contentPanel, "supplier");
        });

        purchaseBtn.addActionListener(e -> {
            orderFrame.refreshData();
            contentLayout.show(contentPanel, "orders");
        });

        logoutBtn.addActionListener(e -> {
            if(user != null && user.getUsername() != null && !user.getUsername().trim().isEmpty()){
                ActivityLogStore.log("User Logged Out: " + user.getUsername().trim());
            }else{
                ActivityLogStore.log("User Logged Out");
            }
            dispose();
            new LoginFrame();
        });

        setVisible(true);
    }

    private JPanel createCard(String title, JLabel valueLabel){

        Color cardBg = UIManager.getColor("Table.background");
        if(cardBg == null){
            cardBg = Color.WHITE;
        }

        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setOpaque(true);
        card.setBackground(cardBg);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel t = new JLabel(title);
        t.setOpaque(false);
        t.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        t.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        t.setForeground(new Color(45, 70, 92));
        t.setHorizontalAlignment(SwingConstants.LEFT);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setOpaque(false);
        valueLabel.setForeground(new Color(22, 52, 80));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        valueLabel.setVerticalAlignment(SwingConstants.TOP);

        card.add(t,BorderLayout.NORTH);
        card.add(valueLabel,BorderLayout.CENTER);

        return card;

    }

    private void makeMenuButtonPlain(JButton button){
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color normalColor = Color.WHITE;
        Color hoverColor  = new Color(180, 200, 255);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(normalColor);
            }
        });
    }

    private void updateTotalProducts(JLabel totalProductsValue){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) AS total FROM products");
                ResultSet rs = pst.executeQuery()
        ){

            if(rs.next()){
                totalProductsValue.setText(String.valueOf(rs.getInt("total")));
            }

        }catch(Exception e){
            e.printStackTrace();
            totalProductsValue.setText("0");
        }
    }

    private void updateLowStockItems(JLabel lowStockValue){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) AS total FROM products WHERE quantity <= reorder_level");
                ResultSet rs = pst.executeQuery()
        ){

            if(rs.next()){
                lowStockValue.setText(String.valueOf(rs.getInt("total")));
            }

        }catch(Exception e){
            e.printStackTrace();
            lowStockValue.setText("0");
        }
    }

    private void updateActiveSuppliers(JLabel activeSuppliersValue){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) AS total FROM suppliers WHERE LOWER(TRIM(COALESCE(status,'Active')))='active'");
                ResultSet rs = pst.executeQuery()
        ){

            if(rs.next()){
                activeSuppliersValue.setText(String.valueOf(rs.getInt("total")));
            }

        }catch(Exception e){
            e.printStackTrace();
            activeSuppliersValue.setText("0");
        }
    }

    private void updateRecentOrders(JLabel recentOrdersValue){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement(
                    "SELECT COUNT(*) AS total FROM purchase_orders " +
                    "WHERE MONTH(order_date) = MONTH(CURDATE()) AND YEAR(order_date) = YEAR(CURDATE())"
                );
                ResultSet rs = pst.executeQuery()
        ){

            if(rs.next()){
                recentOrdersValue.setText(String.valueOf(rs.getInt("total")));
            }

        }catch(Exception e){
            e.printStackTrace();
            recentOrdersValue.setText("0");
        }
    }

    private void ensureSupplierStatusColumnExists(){

        try(
                Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement("ALTER TABLE suppliers ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'Active'")
        ){
            pst.executeUpdate();
        }catch(Exception e){
            // Ignore if column already exists.
            if(!(e instanceof java.sql.SQLException) || ((java.sql.SQLException) e).getErrorCode() != 1060){
                e.printStackTrace();
            }
        }
    }

    private void refreshActivityList(DefaultListModel<String> activityListModel, String selectedFilter){

        ActivityLogStore.Period period;

        if("Day".equalsIgnoreCase(selectedFilter)){
            period = ActivityLogStore.Period.DAY;
        }else if("Month".equalsIgnoreCase(selectedFilter)){
            period = ActivityLogStore.Period.MONTH;
        }else{
            period = ActivityLogStore.Period.WEEK;
        }

        List<ActivityEntry> entries = ActivityLogStore.getEntries(period);
        activityListModel.clear();

        if(entries.isEmpty()){
            activityListModel.addElement("No activity found for this period.");
            return;
        }

        for(ActivityEntry entry : entries){
            String timestamp = entry.getTimestamp().format(ACTIVITY_TIME_FORMAT);
            activityListModel.addElement(timestamp + " - " + entry.getDescription());
        }
    }

    private void styleActivityFilter(JComboBox<String> activityFilter){

        Color filterBorderColor = new Color(148, 167, 186);

        activityFilter.setFont(new Font("Segoe UI", Font.BOLD, 12));
        activityFilter.setForeground(new Color(25, 56, 87));
        activityFilter.setBackground(Color.WHITE);
        activityFilter.setFocusable(false);
        activityFilter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        activityFilter.setMaximumRowCount(3);
        activityFilter.setPreferredSize(new Dimension(112, 30));
        activityFilter.setBorder(BorderFactory.createLineBorder(filterBorderColor));
        activityFilter.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrowButton = new BasicArrowButton(
                    SwingConstants.SOUTH,
                    Color.WHITE,
                    Color.WHITE,
                    new Color(96, 120, 144),
                    Color.WHITE
                );
                arrowButton.setBorder(BorderFactory.createEmptyBorder());
                arrowButton.setContentAreaFilled(false);
                arrowButton.setOpaque(true);
                arrowButton.setBackground(Color.WHITE);
                arrowButton.setPreferredSize(new Dimension(18, 16));
                arrowButton.setMinimumSize(new Dimension(18, 16));
                arrowButton.setMaximumSize(new Dimension(18, 16));
                return arrowButton;
            }
        });

        activityFilter.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(4, 7, 4, 7));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));

                if(index == -1) {
                    label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    label.setForeground(new Color(25, 56, 87));
                    label.setBackground(Color.WHITE);
                } else if(isSelected) {
                    label.setBackground(new Color(232, 240, 248));
                    label.setForeground(new Color(20, 52, 84));
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(new Color(35, 62, 88));
                }

                return label;
            }
        });
    }

}