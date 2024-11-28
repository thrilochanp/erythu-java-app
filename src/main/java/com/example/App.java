package com.example;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

public class App {
    public static void main(String[] args) {
        // Initialize JPA Entity Manager
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("myJpaUnit");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            System.out.println("JPA Transaction started...");
            
            // Perform your entity operations here
            // Example: Create or fetch entities
            System.out.println("Performing entity operations...");
            
            em.getTransaction().commit();
            System.out.println("JPA Transaction committed successfully.");
        } catch (Exception e) {
            System.err.println("Error during JPA operations: " + e.getMessage());
            em.getTransaction().rollback();
        } finally {
            em.close();
            emf.close();
        }
    }
}

// Define the JAX-RS Application path
@ApplicationPath("/")
class RestApplication extends Application {}

// REST API endpoint to handle requests
@Path("/")
class WelcomeResource {

    @GET
    public Response welcomeMessage() {
        String message = "Welcome to e-rythu portal";
        return Response.ok(message).build();
    }
}
