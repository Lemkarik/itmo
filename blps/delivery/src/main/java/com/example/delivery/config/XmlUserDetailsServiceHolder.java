package com.example.delivery.config;


public class XmlUserDetailsServiceHolder {

    private static XmlUserDetailsService service;

    public static void setService(XmlUserDetailsService service) {
        XmlUserDetailsServiceHolder.service = service;
    }

    public static XmlUserDetailsService getService() {
        return service;
    }
}
