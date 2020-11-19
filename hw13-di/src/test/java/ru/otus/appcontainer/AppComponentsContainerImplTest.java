package ru.otus.appcontainer;

import junit.framework.TestCase;
import ru.otus.appcontainer.api.AppComponentsContainer;
import ru.otus.services.EquationPreparer;
import ru.otus.services.EquationPreparerImpl;
import ru.otus.services.IOService;
import ru.otus.services.IOServiceConsole;

import static org.assertj.core.api.Assertions.*;

public class AppComponentsContainerImplTest extends TestCase {

    private AppComponentsContainer appComponentsContainer;

    @Override
    public void setUp() throws Exception {
        appComponentsContainer = new AppComponentsContainerImpl(TestAppConfig1.class);
    }

    public void testGetAppComponent() {
        assertThat(appComponentsContainer.getAppComponent(IOService.class)).isInstanceOf(IOServiceConsole.class);
        assertThat(appComponentsContainer.getAppComponent(EquationPreparer.class)).isInstanceOf(EquationPreparerImpl.class);
    }

    public void testTestGetAppComponent() {
        var component1 = appComponentsContainer.getAppComponent("testBean2");
        var component2 = appComponentsContainer.getAppComponent("testBean1");

        assertThat(component1).isInstanceOf(EquationPreparer.class);
        assertThat(component2).isInstanceOf(IOService.class);
    }
}