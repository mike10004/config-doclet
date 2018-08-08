package io.github.mike10004.configdoclet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Reflections {

    private static final Class<?>[] NO_PARAMS = {};
    private static final Object[] NO_ARGS = {};

    private Reflections() {}

    public static <T> T newInstanceNoArgs(Class<? extends T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return clazz.getConstructor().newInstance();
    }

    public static <T> T invokeNoArgs(Object instance, String methodName) throws ReflectiveOperationException {
        return invokeNoArgs(instance.getClass(), instance, methodName);
    }

    public static <T> T invokeNoArgs(Class<?> clazz, Object instance, String methodName) throws ReflectiveOperationException {
        return invoke(clazz, instance, methodName, NO_PARAMS, NO_ARGS);
    }

    public static <T> T invokeStaticNoArgs(Class<?> clazz, String methodName) throws ReflectiveOperationException {
        return invokeStatic(clazz, methodName, NO_PARAMS, NO_ARGS);
    }

    public static <T> T invokeStatic(Class<?> clazz, String methodName, Class<?>[] methodParameterTypes, Object[] args) throws ReflectiveOperationException {
        return invoke(clazz, null, methodName, methodParameterTypes, args);
    }

    public static <T, P> T invokeOneArg(Object instance, String methodName, Class<P> methodParameterType, P arg) throws ReflectiveOperationException {
        return invokeOneArg(instance.getClass(), instance, methodName, methodParameterType, arg);
    }

    public static <T, P> T invokeOneArg(Class<?> clazz, Object instance, String methodName, Class<P> methodParameterType, P arg) throws ReflectiveOperationException {
        return invoke(clazz, instance, methodName, new Class[]{methodParameterType}, new Object[]{arg});
    }

    public static <T> T invoke(Object instance, String methodName, Class<?>[] methodParameterTypes, Object[] args) throws ReflectiveOperationException {
        return invoke(instance.getClass(), instance, methodName, methodParameterTypes, args);
    }

    public static <T> T invoke(Class<?> clazz, Object instance, String methodName, Class<?>[] methodParameterTypes, Object[] args) throws ReflectiveOperationException {
        Method method = clazz.getDeclaredMethod(methodName, methodParameterTypes);
        method.setAccessible(true);
        Object returnValue = method.invoke(instance, args);
        //noinspection unchecked
        return (T) returnValue;
    }
}
