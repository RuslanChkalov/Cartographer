package org.example.cartographer.controller;


import org.example.cartographer.domain.Route;
import org.example.cartographer.repos.RouteRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Controller
@SessionAttributes({"userPointList", "username"})
public class MainPageController {
    @Autowired
    private RouteRepo routeRepo;

    //Точка входа на главную страницу, запрос имени пользователя
    @GetMapping("/")
    public String main(@RequestParam(name = "username", required = false) String name,
                       Model model) {
        model.addAttribute("userPointList", new ArrayList<>());
        model.addAttribute("username", getAuthUsername());
        return "main";
    }

    //Сохранение текущего маршрута в БД
    @RequestMapping(value = "/requests/saveRoute", method = RequestMethod.POST)
    @ResponseBody
    public String routeSave(@RequestParam("routeName") String routeName,
                       @RequestParam("routeNote") String routeNote,
                       @ModelAttribute("username") String username,
                       @ModelAttribute("userPointList") ArrayList<String> pointList
    ) {
        if (pointList.isEmpty() || pointList.get(0).isEmpty()) {
            return "error1";
        }
        if (routeName.isEmpty()) {
            return "error2";
        }
        for (Route buffer : routeRepo.findByCreatorName(username)) {
            if (buffer.getName().equals(routeName)) {
                return "error3";
            }
        }
        Route route = new Route(routeNote, routeName, username, pointList);
        routeRepo.save(route);
        return "ok";
    }

    //Выгрузка координат точек из скрипта Яндекс.Карт
    @RequestMapping(value = "/requests/getWayPoints", method = RequestMethod.POST)
    @ResponseBody
    public String getWaypoints(@RequestParam("message") String message, Model model) {
        String buffer[] = message.split("_");
        ArrayList<String> pointList = new ArrayList<>();
        for (int i = 0; i < buffer.length; i++) {
            pointList.add(buffer[i]);
        }
        model.addAttribute("userPointList", pointList);
        return message;
    }

    //Добавление новой строки для ввода точки маршрута вкладки "Маршрут"
    @RequestMapping(value = "/requests/addLineInTable", method = RequestMethod.GET)
    public String addLineInTable(@ModelAttribute("userPointList") ArrayList<String> pointList, Model model) {
        if (pointList.size() == 0 || !pointList.get(pointList.size() - 1).isEmpty()) {
            pointList.add("");
        }
        model.addAttribute("points", pointList);
        return "main_routeTab :: #pointsTable";
    }

    //Сопоставление списка точек на Яндекс.Карте со списком координат в таблице раздела "Маршрут"
    @RequestMapping(value = "/requests/updateTable", method = RequestMethod.GET)
    public String autoUpdateTable(@ModelAttribute("userPointList") ArrayList<String> pointList, Model model) {
        model.addAttribute("points", pointList);
        return "main_routeTab :: #pointsTable";
    }

    //Получить имя пользователя сессии
    public String getAuthUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return username;
    }

    //Вывод списка архивных маршрутов для данного пользователя
    @RequestMapping(value = "/requests/refreshRouteArchive", method = RequestMethod.POST)
    public String refreshRouteArchive(@ModelAttribute("username") String username,
                                      @RequestParam("sortingVariant") int sortingVariant,
                                      Model model) {
        List<List<String>> finalList = new ArrayList<List<String>>();
        if (sortingVariant == 1) {
            for (Route buffer : routeRepo.findByCreatorName(username)) {
                finalList.add(new ArrayList<String>(Arrays.asList
                        (buffer.getId().toString(), buffer.getName(), buffer.getNote())));
            }
            Collections.reverse(finalList);
        } else if (sortingVariant == 2) {
            for (Route buffer : routeRepo.findByCreatorNameOrderByName(username)) {
                finalList.add(new ArrayList<String>(Arrays.asList
                        (buffer.getId().toString(), buffer.getName(), buffer.getNote())));
            }
        } else {
            for (Route buffer : routeRepo.findByCreatorNameOrderByNote(username)) {
                finalList.add(new ArrayList<String>(Arrays.asList
                        (buffer.getId().toString(), buffer.getName(), buffer.getNote())));
            }
            Collections.reverse(finalList);
        }
        model.addAttribute("dataList", finalList);
        return "main_controlTab :: #archiveRoutesTable";
    }

    //Удаление маршрута из архива
    @RequestMapping(value = "/requests/removeArchiveRoute", method = RequestMethod.POST)
    @ResponseBody
    public void removeArchiveRoute(@RequestParam("routeId") long elementId) {
        routeRepo.deleteById(elementId);
    }

    //Загрузка архивного маршрута на карту
    @RequestMapping(value = "/requests/loadArchiveRoute", method = RequestMethod.POST)
    @ResponseBody
    public List<String> setWaypoints(@RequestParam("routeId") long routeId, Model model) {
        List<String> routeList = new ArrayList<>();
        for (Route buffer : routeRepo.findById(routeId)) {
            routeList = buffer.getPointsList();
        }
        model.addAttribute("userPointList", routeList);
        return routeList;
    }

    //Сессия истекла - редирект на страницу авторизации
    @ExceptionHandler(HttpSessionRequiredException.class)
    public String sessionExpired() {
        return "/login_redirect";
    }
}
