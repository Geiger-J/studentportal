package com.example.studentportal.repository;

import com.example.studentportal.model.Request;
import com.example.studentportal.model.Subject;
import com.example.studentportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// Repository: JPA repository for Request entities
//
// - query requests by user, status, subject, and matched partner
// - clear matched-partner references before user deletion
// - support admin filtering and archiving workflows
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

        List<Request> findByUserOrderByCreatedAtDesc(User user);

        List<Request> findByUserAndArchivedFalseOrderByCreatedAtDesc(User user);

        Optional<Request> findByUserAndSubjectAndTypeAndStatus(User user, Subject subject, String type, String status);

        boolean existsByUserAndSubjectAndTypeAndStatus(User user, Subject subject, String type, String status);

        List<Request> findByStatus(String status);

        List<Request> findByStatusAndArchivedFalse(String status);

        List<Request> findAllByOrderByCreatedAtDesc();

        List<Request> findBySubject(Subject subject);

        List<Request> findAllByArchivedFalse();

        void deleteByUser(User user);

        @Modifying
        // bulk-null matched partner [avoids N+1 on delete]
        @Query("UPDATE Request r SET r.matchedPartner = null WHERE r.matchedPartner = :matchedPartner")
        // bind matchedPartner to JPQL param
        void clearMatchedPartnerReferences(@Param("matchedPartner") User matchedPartner);

        Optional<Request> findByUserAndMatchedPartnerAndStatus(User user, User matchedPartner, String status);

        Optional<Request> findByUserAndMatchedPartnerAndStatusAndSubject(User user, User matchedPartner, String status,
                        Subject subject);

        // requests where another user is the matched partner at a given status
        // used when that partner is being deleted - cancel their outstanding matches
        List<Request> findByMatchedPartnerAndStatus(User matchedPartner, String status);
}
