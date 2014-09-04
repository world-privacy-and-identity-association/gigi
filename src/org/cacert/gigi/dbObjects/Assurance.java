package org.cacert.gigi.dbObjects;

import java.sql.ResultSet;
import java.sql.SQLException;


public class Assurance {
    private int id;

    private User from;

    private User to;

    private String location;

    private String method;

    private int points;

    private String date;

    public Assurance(ResultSet result) throws SQLException {
        super();
        this.id = result.getInt("id");
        this.from = User.getById(result.getInt("from"));
        this.to = User.getById(result.getInt("to"));
        this.location = result.getString("location");
        this.method = result.getString("method");
        this.points = result.getInt("points");
        this.date = result.getString("date");
    }

    public User getFrom() {
        return from;
    }

    public int getId() {
        return id;
    }

    public String getLocation() {
        return location;
    }

    public int getPoints() {
        return points;
    }

    public User getTo() {
        return to;
    }

    public String getMethod() {
        return method;
    }

    public String getDate() {
        return date;
    }

}
