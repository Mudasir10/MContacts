package com.mudasir.mcontacts;

import java.util.HashMap;
import java.util.Map;

public class CloudContacts {

    private String key;
    private String name;
    private String phone;

    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    private boolean expanded;

    public CloudContacts() {
    }

    public CloudContacts(String key, String name, String phone) {
        this.key = key;
        this.name = name;
        this.phone = phone;
        this.expanded=false;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("key", this.key);
        result.put("name", this.name);
        result.put("phone", this.phone);
        return result;
    }
}
