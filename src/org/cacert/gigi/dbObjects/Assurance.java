package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.GigiResultSet;
import org.cacert.gigi.dbObjects.wrappers.DataContainer;

@DataContainer
public class Assurance {

    public enum AssuranceType {
        FACE_TO_FACE("Face to Face Meeting"), TOPUP("TOPUP"), TTP_ASSISTED("TTP-Assisted");

        private final String description;

        private AssuranceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private int id;

    private User from;

    private User to;

    private String location;

    private String method;

    private int points;

    private String date;

    public Assurance(GigiResultSet res) {
        super();
        this.id = res.getInt("id");
        this.from = User.getById(res.getInt("from"));
        this.to = User.getById(res.getInt("to"));
        this.location = res.getString("location");
        this.method = res.getString("method");
        this.points = res.getInt("points");
        this.date = res.getString("date");
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
