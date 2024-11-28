package com.example;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("myJpaUnit");
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            System.out.println("JPA Transaction started...");

            // Perform your entity operations here
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
