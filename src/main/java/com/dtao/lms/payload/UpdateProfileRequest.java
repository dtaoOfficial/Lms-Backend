package com.dtao.lms.payload;

public class UpdateProfileRequest {
    private String name;
    private String phone;
    private String department;
    private String about;

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
}
