package pt.saltosnaspalhacadas.backend.admin;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.material.Material;
import pt.saltosnaspalhacadas.backend.material.MaterialRepository;
import pt.saltosnaspalhacadas.backend.material.api.MaterialResponse;

@RestController
@RequestMapping("/api/v1/admin/materials")
public class AdminMaterialController {
    private final MaterialRepository materials;

    public AdminMaterialController(MaterialRepository materials) {
        this.materials = materials;
    }

    @GetMapping
    List<MaterialResponse> listMaterials() {
        return materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .map(MaterialResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MaterialResponse createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        int displayOrder = materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .mapToInt(Material::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
        Material material = new Material(request.name().trim(), request.imageUrl().trim(), displayOrder);
        return MaterialResponse.from(materials.save(material));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteMaterial(@PathVariable Long id) {
        Material material = materials.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));
        materials.delete(material);
    }

    record CreateMaterialRequest(
            @NotBlank(message = "O nome do material é obrigatório")
            @Size(max = 140, message = "O nome do material pode ter no máximo 140 caracteres")
            String name,
            @NotBlank(message = "A fotografia do material é obrigatória")
            @Size(max = 2048, message = "A URL da fotografia é demasiado longa")
            String imageUrl) {
    }
}
