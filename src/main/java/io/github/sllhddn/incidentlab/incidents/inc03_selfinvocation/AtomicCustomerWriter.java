package io.github.sllhddn.incidentlab.incidents.inc03_selfinvocation;

import io.github.sllhddn.incidentlab.repo.CustomerRepository;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** INC-03 fix: the transactional method lives on its own bean. */
@Component
public class AtomicCustomerWriter {

    private final CustomerRepository customers;

    public AtomicCustomerWriter(CustomerRepository customers) {
        this.customers = customers;
    }

    @Transactional
    public void saveAll(List<String> names) {
        for (String name : names) {
            customers.save(CustomerImportService.validated(name));
        }
    }
}
