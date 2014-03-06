package org.hwbot.prime.model;

public class Result {

    @Override
    public String toString() {
        return "Result [" + (user != null ? "user=" + user + ", " : "") + (team != null ? "team=" + team + ", " : "")
                + (hardware != null ? "hardware=" + hardware + ", " : "") + (score != null ? "score=" + score + ", " : "")
                + (points != null ? "points=" + points + ", " : "") + (country != null ? "country=" + country + ", " : "") + (id != null ? "id=" + id : "")
                + "]";
    }

    public String user, team, hardware, score, points, country;
    public Integer id;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getHardware() {
        return hardware;
    }

    public void setHardware(String hardware) {
        this.hardware = hardware;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getPoints() {
        return points;
    }

    public void setPoints(String points) {
        this.points = points;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

}