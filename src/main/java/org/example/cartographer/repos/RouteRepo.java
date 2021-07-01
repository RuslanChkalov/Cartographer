package org.example.cartographer.repos;

import org.example.cartographer.domain.Route;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RouteRepo extends CrudRepository<Route, Long> {
    List<Route> findByCreatorNameOrderByNote(String creatorName);
    List<Route> findByCreatorName(String creatorName);
    List<Route> findByName(String routeName);
    List<Route> findById(long id);
    List<Route> findByCreatorNameOrderByName(String creatorName);
}
