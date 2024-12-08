package com.micro.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(
        info = @Info(
                title = "${api.info.title}",
                description = "${api.info.description}",
                version = "${api.info.version}",
                contact = @Contact(
                        name = "${api.info.contact.name}",
                        email = "${api.info.contact.email}",
                        url = "${api.info.contact.url}"
                ),
                termsOfService = "${api.info.title}"
        )
)
public class OpenApiConfiguration {
}
