package pt.saltosnaspalhacadas.backend.material.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.material.MaterialRepository;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {
    private final MaterialRepository materials;
    private final ApiResponseLimits responseLimits;

    public MaterialController(MaterialRepository materials, ApiResponseLimits responseLimits) {
        this.materials = materials;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    List<MaterialResponse> listMaterials() {
        return responseLimits.publicList(materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .map(MaterialResponse::from));
    }
}
