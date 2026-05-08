package com.closetruth.pet;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @GetMapping
    public List<PetResponse> listPets() {
        return petService.listPets();
    }

    @GetMapping("/shop")
    public Map<String, Object> getShopInfo() {
        return petService.getShopInfo();
    }

    @PostMapping("/adopt")
    public PetResponse adoptPet(@RequestParam String name, @RequestParam String type) {
        return petService.adoptPet(name, type);
    }

    @PostMapping("/{id}/feed")
    public PetResponse feedPet(@PathVariable Long id) {
        return petService.feedPet(id);
    }

    @PostMapping("/{id}/play")
    public PetResponse playWithPet(@PathVariable Long id) {
        return petService.playWithPet(id);
    }

    @PostMapping("/{id}/train")
    public PetResponse trainPet(@PathVariable Long id) {
        return petService.trainPet(id);
    }

    @PostMapping("/{id}/rest")
    public PetResponse restPet(@PathVariable Long id) {
        return petService.restPet(id);
    }

    @PostMapping("/{id}/delete")
    public Map<String, String> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return Map.of("message", "宠物已送走");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadRequest(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
