package pt.saltosnaspalhacadas.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import pt.saltosnaspalhacadas.backend.media.LocalMediaStorage;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {
    private final LocalMediaStorage storage;
    public MediaResourceConfig(LocalMediaStorage storage) { this.storage = storage; }
    @Override public void addResourceHandlers(ResourceHandlerRegistry registry) { registry.addResourceHandler("/api/v1/media/**").addResourceLocations(storage.getDirectory().toUri().toString() + "/"); }
}
