package com.example;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class App {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("myJpaUnit");
        EntityManager em = emf.createEntityManager();

        em.getTransaction().begin();
        // Add your entity operations here, like creating, fetching, etc.
        em.getTransaction().commit();

        em.close();
        emf.close();
    }
}
