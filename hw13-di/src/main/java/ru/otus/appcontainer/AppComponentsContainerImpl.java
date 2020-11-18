package ru.otus.appcontainer;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

import java.lang.reflect.Method;
import java.util.*;

public class AppComponentsContainerImpl implements AppComponentsContainer {

    private final List<Object> appComponents = new ArrayList<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();
    private final String orderNameSeparator = "&&&&";
    private Object configInstance;

    public AppComponentsContainerImpl(Class<?> initialConfigClass) {
        processConfig(initialConfigClass);
    }

    private void processConfig(Class<?> configClass) {
        checkConfigClass(configClass);
        try {
            configInstance = configClass.getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Method[] methods = configClass.getDeclaredMethods();
        if (methods == null || methods.length == 0) {
            throw new IllegalArgumentException("Empty config class");
        }

        var methodsMap = createMethodsMap(methods);
        createBeans(methodsMap);
    }

    private Map<String, Method> createMethodsMap(Method[] methods) {
        Map<String, Method> methodsMap = new TreeMap<>();
        Arrays.stream(methods).forEach(method -> {
            if (!method.isAnnotationPresent(AppComponent.class)) {
                return;
            }

            var order = method.getAnnotation(AppComponent.class).order();
            var name = method.getAnnotation(AppComponent.class).name();
            methodsMap.put(order + orderNameSeparator + name, method);
        });

        return methodsMap;
    }

    private void createBeans(Map<String, Method> methodsMap) {
        methodsMap.forEach((orderName, method) -> {
            String name = orderName.split(orderNameSeparator)[1];
            var bean = createBean(method);
            appComponents.add(bean);
            appComponentsByName.put(name, bean);
        });
    }

    private Object createBean(Method method) {
        method.setAccessible(true);
        var constructorParameterTypes = method.getGenericParameterTypes();

        try {
            if (constructorParameterTypes.length == 0) {
                return method.invoke(configInstance);
            }

            var paramsArray = Arrays.stream(constructorParameterTypes).map(item -> getAppComponent(getClassByName(item.getTypeName()))).toArray();

            return method.invoke(configInstance, paramsArray);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Class<?> getClassByName(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkConfigClass(Class<?> configClass) {
        if (!configClass.isAnnotationPresent(AppComponentsContainerConfig.class)) {
            throw new IllegalArgumentException(String.format("Given class is not config %s", configClass.getName()));
        }
    }

    @Override
    public <C> C getAppComponent(Class<C> componentClass) {
        var bean = appComponents.stream().filter(componentClass::isInstance).findFirst().orElseThrow();

        return (C) bean;
    }

    @Override
    public <C> C getAppComponent(String componentName) {
        var returnEntry = appComponentsByName.entrySet().stream().filter(entry -> componentName.equals(entry.getKey())).findFirst().orElseThrow();

        return (C) returnEntry.getValue();
    }
}
