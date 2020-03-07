package gr.forth.ics.isl.elas4rdfrest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@SpringBootApplication
public class Elas4rdfRestApplication {
    public static void main(String[] args) {
        SpringApplication.run(Elas4rdfRestApplication.class, args);
    }
}
