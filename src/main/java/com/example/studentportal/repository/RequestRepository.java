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

/**
 * Repository interface for Request entity operations.
 */
@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByUserOrderByCreatedAtDesc(User user);

    Optional<Request> findByUserAndSubjectAndTypeAndStatus(
        User user, Subject subject, String type, String status);

    boolean existsByUserAndSubjectAndTypeAndStatus(
        User user, Subject subject, String type, String status);

    List<Request> findByStatus(String status);

    List<Request> findBySubject(Subject subject);

    List<Request> findAllByArchivedFalse();

    void deleteByUser(User user);

    @Modifying
    @Query("UPDATE Request r SET r.matchedPartner = null WHERE r.matchedPartner = :matchedPartner")
    void clearMatchedPartnerReferences(@Param("matchedPartner") User matchedPartner);

    Optional<Request> findByUserAndMatchedPartnerAndStatus(
        User user, User matchedPartner, String status);

    Optional<Request> findByUserAndMatchedPartnerAndStatusAndSubject(
        User user, User matchedPartner, String status, Subject subject);
}
