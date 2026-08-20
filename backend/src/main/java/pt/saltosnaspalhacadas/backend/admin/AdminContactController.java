package pt.saltosnaspalhacadas.backend.admin;

import java.net.URI;
import java.util.Locale;
import java.util.regex.Pattern;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9()\\s-]+$");
    private static final Pattern INSTAGRAM_PATTERN = Pattern.compile(
            "^(?:(?:https?://)?(?:www\\.)?instagram\\.com/[A-Za-z0-9._]{1,30}/?|@?[A-Za-z0-9._]{1,30})$",
            Pattern.CASE_INSENSITIVE);

    private final ContactRepository contacts;

    public AdminContactController(ContactRepository contacts) {
        this.contacts = contacts;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse createContact(@Valid @RequestBody CreateContactRequest request) {
        validateContactValue(request.type(), request.value());
        Contact contact = new Contact(request.label().trim(), request.type(), request.value().trim(), 0);
        return ContactResponse.from(contacts.save(contact));
    }

    @PutMapping("/{id}")
    ContactResponse updateContact(@PathVariable Long id, @Valid @RequestBody UpdateContactRequest request) {
        validateContactValue(request.type(), request.value());
        Contact contact = contacts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contacto não encontrado"));

        contact.update(request.label().trim(), request.type(), request.value().trim());
        return ContactResponse.from(contacts.save(contact));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteContact(@PathVariable Long id) {
        Contact contact = contacts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contacto não encontrado"));
        contacts.delete(contact);
    }

    private static void validateContactValue(ContactType type, String value) {
        String normalized = value.trim();
        boolean valid = switch (type) {
            case EMAIL -> EMAIL_PATTERN.matcher(normalized).matches();
            case PHONE, WHATSAPP -> isValidPhoneNumber(normalized);
            case INSTAGRAM -> INSTAGRAM_PATTERN.matcher(normalized).matches();
            case WEBSITE -> isValidWebsite(normalized);
        };

        if (!valid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, contactValueError(type));
        }
    }

    private static boolean isValidPhoneNumber(String value) {
        if (!PHONE_PATTERN.matcher(value).matches()) {
            return false;
        }

        int digitCount = value.replaceAll("\\D", "").length();
        return digitCount >= 6 && digitCount <= 15;
    }

    private static boolean isValidWebsite(String value) {
        String candidate = value.toLowerCase(Locale.ROOT).startsWith("http://") || value.toLowerCase(Locale.ROOT).startsWith("https://")
                ? value
                : "https://" + value;

        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String contactValueError(ContactType type) {
        return switch (type) {
            case EMAIL -> "Indica um endereço de email válido";
            case PHONE -> "Indica um número de telefone válido";
            case WHATSAPP -> "Indica um número de WhatsApp válido";
            case INSTAGRAM -> "Indica um utilizador ou URL de Instagram válido";
            case WEBSITE -> "Indica um URL de website válido";
        };
    }

    record CreateContactRequest(
            @NotBlank(message = "O nome do contacto é obrigatório")
            @Size(max = 80, message = "O nome do contacto pode ter no máximo 80 caracteres")
            String label,
            @NotNull(message = "Seleciona o tipo de contacto")
            ContactType type,
            @NotBlank(message = "O contacto é obrigatório")
            @Size(max = 500, message = "O contacto pode ter no máximo 500 caracteres")
            String value) {
    }

    record UpdateContactRequest(
            @NotBlank(message = "O nome do contacto é obrigatório")
            @Size(max = 80, message = "O nome do contacto pode ter no máximo 80 caracteres")
            String label,
            @NotNull(message = "Seleciona o tipo de contacto")
            ContactType type,
            @NotBlank(message = "O contacto é obrigatório")
            @Size(max = 500, message = "O contacto pode ter no máximo 500 caracteres")
            String value) {
    }
}
