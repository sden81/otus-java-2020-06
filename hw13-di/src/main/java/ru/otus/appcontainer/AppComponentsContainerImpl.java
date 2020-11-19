package ru.otus.appcontainer;

import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class AppComponentsContainerImpl implements AppComponentsContainer {

    private final List<Object> appComponents = new ArrayList<>();
    private final Map<String, Object> appComponentsByName = new HashMap<>();

    public AppComponentsContainerImpl(Class<?> initialConfigClass) {
        processConfig(initialConfigClass);
    }

    private void processConfig(Class<?> configClass) {
        checkConfigClass(configClass);

        var methods = getConfigMethods(configClass);
        createBeans(methods);
    }

    private Object createConfigInstance(Class<?> configClass) {
        try {
            return configClass.getDeclaredConstructors()[0].newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<Method> getConfigMethods(Class<?> configClass) {
        var methods = Arrays.stream(configClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(AppComponent.class))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("Empty config class");
        }

        methods.sort((first, second) -> {
            var firstOrder = first.getAnnotation(AppComponent.class).order();
            var secondOrder = second.getAnnotation(AppComponent.class).order();

            if (secondOrder - firstOrder > 0) {
                return -1;
            }

            if (secondOrder - firstOrder < 0) {
                return 1;
            }

            return 0;
        });

        return methods;
    }

    private void createBeans(List<Method> methods) {
        methods.forEach(method -> {
            var name = method.getDeclaredAnnotation(AppComponent.class).name();
            var bean = createBean(method);
            appComponents.add(bean);
            appComponentsByName.put(name, bean);
        });
    }

    private Object createBean(Method method) {
        method.setAccessible(true);
        var constructorParameterTypes = method.getParameterTypes();
        var configInstance = createConfigInstance(method.getDeclaringClass());

        try {
            var paramsArray = Arrays.stream(constructorParameterTypes)
                    .map(this::getAppComponent)
                    .toArray();

            return method.invoke(configInstance, paramsArray);
        } catch (Exception e) {
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
        var bean = appComponents.stream()
                .filter(componentClass::isInstance)
                .findFirst()
                .orElseThrow(() -> new AppComponentsContainerException(String.format("Bean by class %s not exist", componentClass.toGenericString())));

        return (C) bean;
    }

    @Override
    public <C> C getAppComponent(String componentName) {
        if (!appComponentsByName.containsKey(componentName)) {
            throw new AppComponentsContainerException(String.format("Bean by name %s not exist", componentName));
        }

        return (C) appComponentsByName.get(componentName);
    }
}
