package pt.saltosnaspalhacadas.backend.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.contact.Contact;
import pt.saltosnaspalhacadas.backend.contact.ContactRepository;
import pt.saltosnaspalhacadas.backend.contact.ContactType;
import pt.saltosnaspalhacadas.backend.contact.api.ContactResponse;

@RestController
@RequestMapping("/api/v1/admin/contacts")
public class AdminContactController {
    private final ContactRepository contacts;

    public AdminContactController(ContactRepository contacts) { this.contacts = contacts; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse createContact(@Valid @RequestBody CreateContactRequest request) {
        return ContactResponse.from(contacts.save(new Contact(request.label(), request.type(), request.value(), request.displayOrder())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteContact(@PathVariable Long id) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contacto não encontrado"));
        contacts.delete(contact);
    }

    record CreateContactRequest(
            @NotBlank @Size(max = 80) String label,
            @NotNull ContactType type,
            @NotBlank @Size(max = 500) String value,
            @Min(0) int displayOrder) { }
}
