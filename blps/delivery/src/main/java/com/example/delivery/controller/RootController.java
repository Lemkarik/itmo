package com.example.delivery.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    private final String camundaWebappUrl;

    public RootController(
            @Value("${app.camunda.webapp-url:http://localhost:8080/camunda}")
            String camundaWebappUrl) {
        this.camundaWebappUrl = camundaWebappUrl.replaceAll("/+$", "");
    }

    @GetMapping("/")
    public String root() {
        return "redirect:" + camundaWebappUrl + "/app/tasklist/default/";
    }
}
