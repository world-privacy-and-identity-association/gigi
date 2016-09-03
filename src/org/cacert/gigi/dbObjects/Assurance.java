package org.cacert.gigi.dbObjects;

import org.cacert.gigi.database.DBEnum;
import org.cacert.gigi.dbObjects.wrappers.DataContainer;

@DataContainer
public class Assurance {

    public enum AssuranceType implements DBEnum {
        FACE_TO_FACE("Face to Face Meeting"), TOPUP("TOPUP"), TTP_ASSISTED("TTP-Assisted"), NUCLEUS("Nucleus Bonus");

        private final String description;

        private AssuranceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String getDBName() {
            return description;
        }
    }

    private int id;

    private User from;

    private Name to;

    private String location;

    private String method;

    private int points;

    private String date;

    private Country country;

    public Assurance(int id, User from, Name to, String location, String method, int points, String date, Country country) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.location = location;
        this.method = method;
        this.points = points;
        this.date = date;
        this.country = country;

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

    public Name getTo() {
        return to;
    }

    public String getMethod() {
        return method;
    }

    public String getDate() {
        return date;
    }

    public Country getCountry() {
        return country;
    }
}
