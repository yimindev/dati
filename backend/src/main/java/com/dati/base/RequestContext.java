package com.dati.base;

import com.dati.auth.authentication.User;

import java.util.HashMap;
import java.util.Map;

public class RequestContext {

    private static final ThreadLocal<Map<String, Object>> context = new ThreadLocal<>();

    public static Map<String, Object> getContext() {
        if (context.get() == null) {
            context.set(new HashMap<>());
        }
        return context.get();
    }

    public static void setUser(User user) {
        getContext().put("user", user);
    }

    public static User getUser() {
        return (User) getContext().get("user");
    }

    /** Clears the thread-local context (must be called after request handling). */
    public static void clear() {
        context.remove();
    }

}
