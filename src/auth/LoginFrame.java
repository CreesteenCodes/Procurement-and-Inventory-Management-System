package auth;

import dashboard.DashboardFrame;
import models.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame(){

        setTitle("Céleste Retail Management");
        setSize(400,300);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4,1,10,10));
        panel.setBorder(BorderFactory.createEmptyBorder(30,40,30,40));

        JLabel title = new JLabel("Céleste Couture Login", JLabel.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JPanel usernameRow = new JPanel(new BorderLayout(10,0));
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setPreferredSize(new Dimension(80, 25));
        usernameRow.add(usernameLabel, BorderLayout.WEST);
        usernameRow.add(usernameField, BorderLayout.CENTER);

        JPanel passwordRow = new JPanel(new BorderLayout(10,0));
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setPreferredSize(new Dimension(80, 25));
        passwordRow.add(passwordLabel, BorderLayout.WEST);
        passwordRow.add(passwordField, BorderLayout.CENTER);

        JButton loginBtn = new JButton("Login");
        JPanel loginRow = new JPanel(new BorderLayout(10,0));
        JLabel loginSpacer = new JLabel();
        loginSpacer.setPreferredSize(new Dimension(80, 25));
        loginRow.add(loginSpacer, BorderLayout.WEST);
        loginRow.add(loginBtn, BorderLayout.CENTER);

        panel.add(title);
        panel.add(usernameRow);
        panel.add(passwordRow);
        panel.add(loginRow);

        add(panel);

        loginBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String username = usernameField.getText();
                char[] passwordChars = passwordField.getPassword();
                String password = new String(passwordChars);

                User user = AuthService.login(username,password);

                Arrays.fill(passwordChars, '\0');
                passwordField.setText("");

                if(user != null){

                    dispose();

                    new DashboardFrame(user);

                }else{

                    JOptionPane.showMessageDialog(null,"Invalid Login");

                }

            }
        });

        setVisible(true);
    }

}