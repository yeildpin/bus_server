package com.bus.server.utils;

public class ClassUtils {

    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        Class<?> theClass = null;
        try {
            theClass = Class.forName(className);
        }
        catch (ClassNotFoundException e1) {
            try {
                theClass = Thread.currentThread().getContextClassLoader().loadClass(className);
            }
            catch (ClassNotFoundException e2) {
                theClass = ClassUtils.class.getClassLoader().loadClass(className);
            }
        }
        return theClass;
    }
}
