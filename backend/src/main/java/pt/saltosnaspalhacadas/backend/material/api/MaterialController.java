package pt.saltosnaspalhacadas.backend.material.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pt.saltosnaspalhacadas.backend.material.MaterialRepository;

@RestController
@RequestMapping("/api/v1/materials")
public class MaterialController {
    private final MaterialRepository materials;

    public MaterialController(MaterialRepository materials) {
        this.materials = materials;
    }

    @GetMapping
    List<MaterialResponse> listMaterials() {
        return materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .map(MaterialResponse::from)
                .toList();
    }
}
