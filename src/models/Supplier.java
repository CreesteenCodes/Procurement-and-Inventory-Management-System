package models;

public class Supplier {

    private int id;
    private String name;
    private String contact;
    private String phone;
    private String email;
    private String address;

    public Supplier(int id,String name,String contact,String phone,String email,String address){
        this.id = id;
        this.name = name;
        this.contact = contact;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    public int getId(){ return id; }
    public String getName(){ return name; }
    public String getContact(){ return contact; }
    public String getPhone(){ return phone; }
    public String getEmail(){ return email; }
    public String getAddress(){ return address; }

}