package pt.saltosnaspalhacadas.backend.contact.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.contact.ContactRepository;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {
    private final ContactRepository contacts;
    private final ApiResponseLimits responseLimits;

    public ContactController(ContactRepository contacts, ApiResponseLimits responseLimits) {
        this.contacts = contacts;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    List<ContactResponse> listContacts() {
        return responseLimits.publicList(contacts.findAllByVisibleTrueOrderByDisplayOrderAscIdAsc().stream().map(ContactResponse::from));
    }
}
