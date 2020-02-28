/* 
 * Copyright (c) 2018 Veeva Systems Inc.
 * All Rights Reserved.
 * The Controlled-Print-Queue is developed and owned by Veeva Systems Inc.;
 * it is not to be copied, reproduced, or transmitted in any form, by any means, in whole or in part,
 * and may only be used in connection with the deliverable with which it was provided to Customer.
 */
package com.veeva.controlledprintqueue;

import java.util.Properties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.boot.web.support.SpringBootServletInitializer;


/**
 * Spring Boot Application Class
 * @author amol.chinchalkar
 *
 */
@SpringBootApplication

public class Application extends SpringBootServletInitializer {

	/**
	 * Set up Spring Boot Application
	 */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder springApplicationBuilder) {
        return springApplicationBuilder
                .sources(Application.class)
                .properties(getProperties());
    }

    /**
     * Main Method
     * @param args
     */
    public static void main(String[] args) {

        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(Application.class)
        		.properties(getProperties());
        springApplicationBuilder.run(args);
        
    }

    /**
     * Get the properties that Spring Boot needs
     * @return
     */
   static Properties getProperties() {
	  Properties props = new Properties();
      props.put("spring.config.location", "file:${user.home}/cpq_application.properties");      
      return props;
   }
}