package pt.saltosnaspalhacadas.backend.profile;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ProfileNotFoundException extends ResponseStatusException {
    public ProfileNotFoundException(String slug) {
        super(HttpStatus.NOT_FOUND, "Perfil não encontrado");
    }
}
