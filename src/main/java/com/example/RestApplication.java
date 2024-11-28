package com.example;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

// Define the base URL for your REST services (e.g., "/api")
@ApplicationPath("/")
public class RestApplication extends Application {
    // No additional configuration is needed here for simple applications.
    public RestApplication() {
        // Ensure a public constructor exists
        super();
    }
}
