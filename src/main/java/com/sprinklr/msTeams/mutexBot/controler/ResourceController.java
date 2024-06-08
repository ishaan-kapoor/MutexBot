package com.sprinklr.msTeams.mutexBot.controler;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sprinklr.msTeams.mutexBot.model.Resource;
import com.sprinklr.msTeams.mutexBot.service.ResourceService;

@RestController
public class ResourceController {
  @Autowired
  private ResourceService service;

  @GetMapping("/resources")
  public void get() {
    System.out.println("getting");
    List<Resource> all = service.getAll();
    // System.out.println(all);
    for (Resource u: all) {
      System.out.println(u.toString());
    }
    System.out.println("got");
    // Resource got;
    // try {
    //   got = service.find("naam");
    // } catch (Exception e) {
    //   e.printStackTrace();
    //   return;
    // }
    // System.out.println(got);
    // System.out.println("got");
    // System.out.println("saving");
    // got.reserve("ishaan", LocalDateTime.now().plusDays(1));
    // service.save(got);
    // System.out.println("saved");
    // System.out.println("getting");
    // List<Resource> all = service.getAll();
    // System.out.println(all);
    // for (Resource u: all) {
    //   System.out.println(u);
    // }
    // System.out.println("got");
  }
}

