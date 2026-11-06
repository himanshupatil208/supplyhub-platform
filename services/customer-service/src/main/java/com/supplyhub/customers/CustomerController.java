package com.supplyhub.customers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository repo;

    public CustomerController(CustomerRepository repo) {
        this.repo = repo;
    }

    public record CustomerRequest(@NotBlank String name, @Email @NotBlank String email, String phone, String company,
                                  String status) {
    }

    public record CustomerResponse(Long id, String name, String email, String phone, String company, String status) {
        static CustomerResponse from(Customer c) {
            return new CustomerResponse(c.getId(), c.getName(), c.getEmail(), c.getPhone(), c.getCompany(), c.getStatus());
        }
    }

    @GetMapping
    public List<CustomerResponse> all() {
        return repo.findAll().stream().map(CustomerResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerResponse one(@PathVariable Long id) {
        return CustomerResponse.from(find(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse create(@Valid @RequestBody CustomerRequest r) {
        if (repo.findByEmail(r.email()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        return CustomerResponse.from(repo.save(new Customer(r.name(), r.email(), r.phone(), r.company())));
    }

    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id, @Valid @RequestBody CustomerRequest r) {
        Customer c = find(id);
        var existing = repo.findByEmail(r.email());
        if (existing.isPresent() && !existing.get().getId().equals(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        c.update(r.name(), r.email(), r.phone(), r.company(), r.status() == null ? "ACTIVE" : r.status());
        return CustomerResponse.from(repo.save(c));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repo.delete(find(id));
    }

    private Customer find(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}
