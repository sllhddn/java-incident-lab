package io.github.sllhddn.incidentlab.incidents.inc03_selfinvocation;

import io.github.sllhddn.incidentlab.domain.Customer;
import io.github.sllhddn.incidentlab.repo.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * INC-03. A nightly import was meant to be all-or-nothing. When row 3 of a file was bad,
 * rows 1-2 were still committed, and the re-run after the fix created duplicates.
 */
@Service
public class CustomerImportService {

    private final CustomerRepository customers;
    private final AtomicCustomerWriter atomicWriter;

    public CustomerImportService(CustomerRepository customers, AtomicCustomerWriter atomicWriter) {
        this.customers = customers;
        this.atomicWriter = atomicWriter;
    }

    /**
     * As shipped. {@code this.saveAll(..)} is a plain Java call on the target object, so
     * it never goes through the Spring proxy and {@code @Transactional} on saveAll is
     * ignored. Each {@code repository.save} commits in its own transaction.
     */
    public void importBuggy(List<String> names) {
        this.saveAll(names); // BUG: self-invocation bypasses the transactional proxy
    }

    @Transactional
    public void saveAll(List<String> names) {
        for (String name : names) {
            customers.save(validated(name));
        }
    }

    /** Fixed: the call crosses a bean boundary, so it goes through the proxy. */
    public void importFixed(List<String> names) {
        atomicWriter.saveAll(names);
    }

    static Customer validated(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Customer name is blank");
        }
        return new Customer(name.strip(), "STANDARD");
    }
}
