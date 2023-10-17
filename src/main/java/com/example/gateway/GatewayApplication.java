package com.example.gateway;

import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Autowired
    RouteDefinitionLocator locator;

    @Bean
    public List<GroupedOpenApi> apis() {
        final List<GroupedOpenApi> groups = new ArrayList<>();
        final List<RouteDefinition> definitions = locator.getRouteDefinitions().collectList().block();
        definitions.stream()
                .filter(routeDefinition -> routeDefinition.getId().matches(".*-service"))
                .forEach(
                        routeDefinition -> {
                            final String name = routeDefinition.getId().replaceAll("-service", "");
                            GroupedOpenApi.builder().pathsToMatch("/" + name + "/**").group(name).build();
                        }
                );
        return groups;
    }
}
