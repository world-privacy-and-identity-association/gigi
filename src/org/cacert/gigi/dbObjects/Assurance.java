package org.cacert.gigi.dbObjects;

import org.cacert.gigi.dbObjects.wrappers.DataContainer;

@DataContainer
public class Assurance {

    public enum AssuranceType {
        FACE_TO_FACE("Face to Face Meeting"), TOPUP("TOPUP"), TTP_ASSISTED("TTP-Assisted"), NUCLEUS("Nucleus Bonus");

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

    public Assurance(int id, User from, User to, String location, String method, int points, String date) {
        this.id = id;
        this.from = from;
        this.to = to;
        this.location = location;
        this.method = method;
        this.points = points;
        this.date = date;

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
