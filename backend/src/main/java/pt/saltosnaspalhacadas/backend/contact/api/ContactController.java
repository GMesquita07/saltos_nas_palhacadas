package pt.saltosnaspalhacadas.backend.contact.api;

import java.time.Duration;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.contact.ContactRepository;

@RestController
@RequestMapping("/api/v1/contacts")
public class ContactController {
    private static final CacheControl PUBLIC_CONTACT_CACHE = CacheControl.maxAge(Duration.ofSeconds(60)).cachePublic();

    private final ContactRepository contacts;
    private final ApiResponseLimits responseLimits;

    public ContactController(ContactRepository contacts, ApiResponseLimits responseLimits) {
        this.contacts = contacts;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    ResponseEntity<List<ContactResponse>> listContacts() {
        return ResponseEntity.ok()
                .cacheControl(PUBLIC_CONTACT_CACHE)
                .body(responseLimits.publicList(contacts.findAllByVisibleTrueOrderByDisplayOrderAscIdAsc().stream().map(ContactResponse::from)));
    }
}
