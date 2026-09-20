package pt.saltosnaspalhacadas.backend.admin;

import pt.saltosnaspalhacadas.backend.contact.Contact;
import pt.saltosnaspalhacadas.backend.contact.ContactType;

public record AdminContactResponse(
        Long id,
        String label,
        ContactType type,
        String value,
        int displayOrder,
        boolean visible) {

    public static AdminContactResponse from(Contact contact) {
        return new AdminContactResponse(
                contact.getId(),
                contact.getLabel(),
                contact.getType(),
                contact.getValue(),
                contact.getDisplayOrder(),
                contact.isVisible());
    }
}
