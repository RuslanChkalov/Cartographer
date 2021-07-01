package org.example.cartographer.domain;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "route")
public class Route {
    @Id
    @GeneratedValue(strategy= GenerationType.AUTO)
    private Long id;
    private String note;
    private String name;
    private String creatorName;

    @ElementCollection
    @CollectionTable(name="route_points_list")
    private List<String> pointsList;

    public Route(String note, String name, String creatorName, List<String> pointsList) {
        this.note = note;
        this.name = name;
        this.creatorName = creatorName;
        this.pointsList = pointsList;
    }
    public Route() {
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }
    public void setNote(String note) {
        this.note = note;
    }

    public String getCreatorName() {
        return creatorName;
    }
    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    public List<String> getPointsList() {
        return pointsList;
    }
    public void setPointsList(List<String> pointsList) {
        this.pointsList = pointsList;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}
