package my.project.qri3a.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfiguration {

    @Bean
    public OpenAPI defineOpenApi() {
        Server server = new Server();
        server.setUrl("http://localhost:8081");
        server.setDescription("Qri3a Development Server");

        Contact myContact = new Contact();
        myContact.setName("NFISSI Abderrazzak");
        myContact.setEmail("abderazaknfissi34@gmail.com");

        Info information = new Info()
                .title("Qri3a API")
                .version("1.0")
                .description("The Qri3a API provides endpoints for managing and interacting with the Qri3a system.")
                .contact(myContact);
        return new OpenAPI().info(information).servers(List.of(server));
    }
}