package pt.saltosnaspalhacadas.backend.contact;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findAllByVisibleTrueOrderByDisplayOrderAscIdAsc();
    List<Contact> findAllByOrderByDisplayOrderAscIdAsc();
}
