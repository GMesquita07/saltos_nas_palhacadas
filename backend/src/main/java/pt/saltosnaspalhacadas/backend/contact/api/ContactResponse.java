package pt.saltosnaspalhacadas.backend.contact.api;

import pt.saltosnaspalhacadas.backend.contact.Contact;
import pt.saltosnaspalhacadas.backend.contact.ContactType;

public record ContactResponse(Long id, String label, ContactType type, String value, int displayOrder) {
    public static ContactResponse from(Contact contact) {
        return new ContactResponse(contact.getId(), contact.getLabel(), contact.getType(), contact.getValue(), contact.getDisplayOrder());
    }
}
