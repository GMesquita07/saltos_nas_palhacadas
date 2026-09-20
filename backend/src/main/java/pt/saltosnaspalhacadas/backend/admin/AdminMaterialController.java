package pt.saltosnaspalhacadas.backend.admin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pt.saltosnaspalhacadas.backend.config.ApiResponseLimits;
import pt.saltosnaspalhacadas.backend.material.Material;
import pt.saltosnaspalhacadas.backend.material.MaterialRepository;
import pt.saltosnaspalhacadas.backend.material.api.MaterialResponse;
import pt.saltosnaspalhacadas.backend.security.PublicUrlValidator;

@RestController
@RequestMapping("/api/v1/admin/materials")
public class AdminMaterialController {
    private final MaterialRepository materials;
    private final ApiResponseLimits responseLimits;

    public AdminMaterialController(MaterialRepository materials, ApiResponseLimits responseLimits) {
        this.materials = materials;
        this.responseLimits = responseLimits;
    }

    @GetMapping
    List<MaterialResponse> listMaterials() {
        return responseLimits.adminList(materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .map(MaterialResponse::from));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    MaterialResponse createMaterial(@Valid @RequestBody CreateMaterialRequest request) {
        int displayOrder = materials.findAllByOrderByDisplayOrderAscNameAscIdAsc()
                .stream()
                .mapToInt(Material::getDisplayOrder)
                .max()
                .orElse(-1) + 1;
        Material material = new Material(
                request.name().trim(),
                PublicUrlValidator.required(request.imageUrl(), "Indica um URL de fotografia válido"),
                displayOrder);
        return MaterialResponse.from(materials.save(material));
    }

    @PutMapping("/{id}")
    MaterialResponse updateMaterial(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMaterialRequest request) {
        Material material = materials.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado"));

        material.update(
                request.name().trim(),
                PublicUrlValidator.required(request.imageUrl(), "Indica um URL de fotografia válido"));
        return MaterialResponse.from(materials.save(material));
    }

    @PutMapping("/order")
    List<MaterialResponse> reorderMaterials(@Valid @RequestBody ReorderMaterialsRequest request) {
        if (new HashSet<>(request.materialIds()).size() != request.materialIds().size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A lista de materiais contém repetidos");
        }

        List<Material> currentMaterials = materials.findAllByOrderByDisplayOrderAscNameAscIdAsc();
        Map<Long, Material> byId = currentMaterials.stream().collect(Collectors.toMap(Material::getId, material -> material));
        int displayOrder = 0;

        for (Long id : request.materialIds()) {
            Material material = byId.remove(id);
            if (material == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado");
            }
            material.updateDisplayOrder(displayOrder++);
        }

        for (Material material : byId.values()) {
            material.updateDisplayOrder(displayOrder++);
        }

        return materials.saveAll(currentMaterials)
                .stream()
                .sorted(java.util.Comparator.comparingInt(Material::getDisplayOrder).thenComparing(Material::getName).thenComparing(Material::getId))
                .map(MaterialResponse::from)
                .toList();
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

    record UpdateMaterialRequest(
            @NotBlank(message = "O nome do material é obrigatório")
            @Size(max = 140, message = "O nome do material pode ter no máximo 140 caracteres")
            String name,
            @NotBlank(message = "A fotografia do material é obrigatória")
            @Size(max = 2048, message = "A URL da fotografia é demasiado longa")
            String imageUrl) {
    }

    record ReorderMaterialsRequest(
            @NotEmpty(message = "Envia a nova ordem dos materiais")
            List<Long> materialIds) {
    }
}
