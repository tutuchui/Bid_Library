package atos.admain;

import org.springframework.jdbc.core.RowMapper;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserVO implements RowMapper<UserVO>, Serializable {
    private String name;
    private String pwd;
    private String email;
    private boolean role;
    private String role_string;


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getRole() {
        return role;
    }

    public void setRole(boolean role) {
        this.role = role;
    }

    @Override
    public UserVO mapRow(ResultSet resultSet, int i) throws SQLException {
        UserVO userVO = new UserVO();
        userVO.setEmail(resultSet.getString("Email"));
        userVO.setName(resultSet.getString("Username"));
        userVO.setPwd(resultSet.getString("Password"));
        userVO.setRole(resultSet.getBoolean("Role"));
        userVO.setRole_string(resultSet.getBoolean("Role"));
        return userVO;
    }

    public String getRole_string() {
        return role_string;
    }

    public void setRole_string(boolean isRole) {
        if(isRole){
            this.role_string = "Admin";
        }
        else{
            this.role_string = "User";
        }
    }
}
