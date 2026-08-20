package pt.saltosnaspalhacadas.backend.contact.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.contact.ContactRepository;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {
    private final ContactRepository contacts;

    public ContactController(ContactRepository contacts) { this.contacts = contacts; }

    @GetMapping
    List<ContactResponse> listContacts() {
        return contacts.findAllByVisibleTrueOrderByIdDesc().stream().map(ContactResponse::from).toList();
    }
}
