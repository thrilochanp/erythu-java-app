package com.example;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

// Define the resource URL path (e.g., "/welcome")
@Path("/welcome")
public class WelcomeResource {

    // Handle GET requests and produce plain text responses
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getWelcomeMessage() {
        return "Welcome to e-rythu portal";
    }
}
