package jpql;

import javax.persistence.*;
import java.util.List;

public class JpaMain {
    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("hello");
        PersistenceUnitUtil persistenceUnitUtil = emf.getPersistenceUnitUtil();
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        tx.begin();

        try {
            Member member = new Member();
            member.setUsername("test");
            em.persist(member);

            /**
             * 반환 타입이 명확할때는 TypedQuery
             * 명확하지 않을때는 Query
             * */
            TypedQuery<Member> query = em.createQuery("select m from Member m", Member.class);
            Query query1 = em.createQuery("select m.username, m.age from Member m");

            /** 결과가 없으면 빈 리스트 반환 */
            List<Member> resultList = query.getResultList();

            /** 결과가 정확히 하나가 나와야함.
             *  결과가 없으면 -> NoResultException
             *  결과가 두개 이상 -> NonUniqueResultException
             * */
            Member result = query.getSingleResult();

            /** 파라미터 바인딩 */
            Member singleResult = em.createQuery("select m from Member m where m.username = :username", Member.class)
                    .setParameter("username", "test")
                    .getSingleResult();


            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
}
