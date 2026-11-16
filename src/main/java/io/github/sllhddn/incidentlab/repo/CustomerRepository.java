package io.github.sllhddn.incidentlab.repo;

import io.github.sllhddn.incidentlab.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    long countByNameStartingWith(String prefix);
}
