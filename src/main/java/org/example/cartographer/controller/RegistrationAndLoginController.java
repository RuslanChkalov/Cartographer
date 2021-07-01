package org.example.cartographer.controller;

import org.example.cartographer.domain.Role;
import org.example.cartographer.domain.User;
import org.example.cartographer.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Collections;
import java.util.Map;

@Controller
public class RegistrationAndLoginController {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/registration")
    public String registration(){
        return "registration";
    }

    @PostMapping("/registration")
    public String addUser(User user, Model model){
        if (!user.getPassword().equals(user.getPasswordConfirm())){
            model.addAttribute("message", "Пароли не совпадают");
            return "registration";
        }
        User userFromDb=userRepo.findByUsername(user.getUsername());
        if (userFromDb!=null){
            model.addAttribute("message", "Пользователь уже существует");
            return "registration";
        }
        user.setActive(true);
        user.setRoles(Collections.singleton(Role.USER));
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepo.save(user);
        return "redirect:/login";
    }

}
