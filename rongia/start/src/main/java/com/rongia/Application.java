package com.rongia;

import io.axoniq.framework.springboot.autoconfig.PersistentStreamAutoConfiguration;
import org.axonframework.extension.springboot.autoconfig.JpaAutoConfiguration;
import org.axonframework.extension.springboot.autoconfig.JpaEventStoreAutoConfiguration;
import org.axonframework.extension.springboot.autoconfig.JpaTransactionAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.RollbackOn;

@SpringBootApplication(
    exclude = {
      PersistentStreamAutoConfiguration.class,
      JpaAutoConfiguration.class,
      JpaEventStoreAutoConfiguration.class,
      JpaTransactionAutoConfiguration.class
    })
@EnableTransactionManagement(rollbackOn = RollbackOn.ALL_EXCEPTIONS)
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
