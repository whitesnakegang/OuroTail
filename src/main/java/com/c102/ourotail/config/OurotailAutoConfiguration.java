package com.c102.ourotail.config;

import com.c102.ourotail.controller.TestController;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ourotail SDK auto-configuration class
 * When this library is added as a dependency to other projects,
 * TestController is automatically registered.
 */
@AutoConfiguration
@ConditionalOnClass(TestController.class)
public class OurotailAutoConfiguration {

    /**
     * Register TestController bean
     * @return TestController instance
     */
    @Bean
    @ConditionalOnMissingBean
    public TestController testController() {
        return new TestController();
    }
}
