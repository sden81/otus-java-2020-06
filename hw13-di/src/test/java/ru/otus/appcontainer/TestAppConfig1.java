package ru.otus.appcontainer;

import ru.otus.appcontainer.api.AppComponent;
import ru.otus.appcontainer.api.AppComponentsContainerConfig;
import ru.otus.services.*;

@AppComponentsContainerConfig(order = 1)
public class TestAppConfig1 {
    @AppComponent(order = 2, name = "testBean2")
    public EquationPreparer equationPreparer(IOService ioService) {
        return new EquationPreparerImpl();
    }

    @AppComponent(order = 1, name = "testBean1")
    public IOService ioService() {
        return new IOServiceConsole(System.out, System.in);
    }
}
