package com.sprinklr.msTeams.mutexBot.controler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprinklr.msTeams.mutexBot.model.User;
import com.sprinklr.msTeams.mutexBot.service.UserService;

// @RestController
// public class UserController {
//   @Autowired
//   private UserService service;
//
//   @GetMapping("/users")
//   public void get() {
//     System.out.println("getting");
//     List<User> all = service.getAll();
//     System.out.println(all);
//     for (User u : all) { System.out.println(u); }
//     System.out.println("got");
//
//     System.out.println("saving");
//     User user = all.get(0);
//     user.setName("nonln");
//     service.save(user);
//     System.out.println("saved");
//     System.out.println("getting");
//     all = service.getAll();
//     System.out.println(all);
//     for (User u : all) {
//       System.out.println(u);
//     }
//     System.out.println("got");
//   }
// }
