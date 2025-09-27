package com.metlife.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class controller {

    @GetMapping("/home")
    public String home()
    {
        return "healthcare-risk-assessment-new.html";
    }

    @GetMapping("/dashboard")
    public String dashboard()
    {
        return "healthcare-dashboard-new.html";
    }

}

