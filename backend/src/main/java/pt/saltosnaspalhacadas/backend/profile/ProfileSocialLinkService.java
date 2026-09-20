package pt.saltosnaspalhacadas.backend.profile;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProfileSocialLinkService {
    private static final Pattern PLATFORM_PATTERN = Pattern.compile("^[A-Z0-9_\\-]{2,40}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public void replaceSocialLinks(Profile profile, List<SocialLinkInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            profile.replaceSocialLinks(List.of());
            return;
        }

        List<ProfileSocialLink> links = new ArrayList<>();
        for (int index = 0; index < inputs.size(); index++) {
            links.add(toSocialLink(inputs.get(index), index));
        }
        profile.replaceSocialLinks(links);
    }

    private static ProfileSocialLink toSocialLink(SocialLinkInput input, int displayOrder) {
        String platform = normalizePlatform(input.platform());
        String label = normalizeLabel(input.label());
        String url = normalizeUrl(platform, input.url());
        return new ProfileSocialLink(platform, label, url, displayOrder);
    }

    private static String normalizePlatform(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Seleciona a plataforma do link social");
        }

        String platform = value.trim().toUpperCase(Locale.ROOT);
        if (!PLATFORM_PATTERN.matcher(platform).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A plataforma do link social é inválida");
        }
        return platform;
    }

    private static String normalizeLabel(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String label = value.trim();
        if (label.length() > 80) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O rótulo do link social pode ter no máximo 80 caracteres");
        }
        return label;
    }

    private static String normalizeUrl(String platform, String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica o URL ou email do link social");
        }

        String candidate = value.trim();
        if (candidate.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O URL do link social é demasiado longo");
        }

        if ("EMAIL".equals(platform)) {
            String email = candidate.toLowerCase(Locale.ROOT);
            if (!EMAIL_PATTERN.matcher(email).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica um email válido para o link social");
            }
            return email;
        }

        try {
            URI uri = URI.create(candidate);
            String scheme = uri.getScheme();
            if (uri.getHost() != null && ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                return candidate;
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica um URL http/https válido para o link social");
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica um URL http/https válido para o link social");
    }

    public record SocialLinkInput(String platform, String label, String url) {
    }
}
