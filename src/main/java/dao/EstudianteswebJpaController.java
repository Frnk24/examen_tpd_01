/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;

import dao.exceptions.NonexistentEntityException;
import dto.Estudiantesweb;
import java.io.Serializable;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 *
 * @author frank
 */
public class EstudianteswebJpaController implements Serializable {

    public EstudianteswebJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Estudiantesweb estudiantesweb) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            em.persist(estudiantesweb);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Estudiantesweb estudiantesweb) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            estudiantesweb = em.merge(estudiantesweb);
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = estudiantesweb.getCodiEstdWeb();
                if (findEstudiantesweb(id) == null) {
                    throw new NonexistentEntityException("The estudiantesweb with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Estudiantesweb estudiantesweb;
            try {
                estudiantesweb = em.getReference(Estudiantesweb.class, id);
                estudiantesweb.getCodiEstdWeb();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The estudiantesweb with id " + id + " no longer exists.", enfe);
            }
            em.remove(estudiantesweb);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Estudiantesweb> findEstudianteswebEntities() {
        return findEstudianteswebEntities(true, -1, -1);
    }

    public List<Estudiantesweb> findEstudianteswebEntities(int maxResults, int firstResult) {
        return findEstudianteswebEntities(false, maxResults, firstResult);
    }

    private List<Estudiantesweb> findEstudianteswebEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Estudiantesweb.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Estudiantesweb findEstudiantesweb(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Estudiantesweb.class, id);
        } finally {
            em.close();
        }
    }

    public int getEstudianteswebCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Estudiantesweb> rt = cq.from(Estudiantesweb.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
