package com.dtao.lms;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LmsApplication {
    public static void main(String[] args) {
        // ✅ Load .env file from project root
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .ignoreIfMissing()
                .load();

        // ✅ Inject all variables into system properties
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        // 🧠 Debug: confirm Mongo URI
        System.out.println(">>> MONGODB_URI from .env = " + System.getProperty("MONGODB_URI"));

        SpringApplication.run(LmsApplication.class, args);
    }
}
