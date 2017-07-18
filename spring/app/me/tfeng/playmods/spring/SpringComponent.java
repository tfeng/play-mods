package me.tfeng.playmods.spring;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.springframework.context.ConfigurableApplicationContext;

import com.google.inject.Injector;

import me.tfeng.toolbox.spring.ApplicationManager;
import play.Application;
import play.inject.ApplicationLifecycle;

/**
 * @author Thomas Feng (huining.feng@gmail.com)
 */
@Singleton
public class SpringComponent {

  private Application application;

  @Inject public SpringComponent(Application application, ApplicationLifecycle lifecycle) {
    this.application = application;

    ApplicationManager applicationManager = getApplicationManager(application);
    applicationManager.processInjection(this);

    Injector injector = application.injector().instanceOf(Injector.class);
    ConfigurableApplicationContext applicationContext = applicationManager.getApplicationContext();
    for (String beanName : applicationContext.getBeanDefinitionNames()) {
      Object bean = applicationContext.getBean(beanName);
      injector.injectMembers(bean);
    }

    applicationManager.start();

    lifecycle.addStopHook(this::onStop);
  }

  protected ApplicationManager getApplicationManager(Application application) {
    return application.injector().instanceOf(ApplicationManager.class);
  }

  private CompletionStage onStop() {
    getApplicationManager(application).stop();
    return CompletableFuture.completedFuture(null);
  }
}
