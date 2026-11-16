package io.github.sllhddn.incidentlab.incidents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sllhddn.incidentlab.PostgresIntegrationTest;
import io.github.sllhddn.incidentlab.incidents.inc03_selfinvocation.CustomerImportService;
import io.github.sllhddn.incidentlab.repo.CustomerRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class Inc03SelfInvocationTest extends PostgresIntegrationTest {

    @Autowired CustomerImportService importer;
    @Autowired CustomerRepository customers;

    @Test
    void reproduce_partialImportIsCommitted() {
        String prefix = "inc03-" + UUID.randomUUID() + "-";
        List<String> file = List.of(prefix + "a", prefix + "b", "   ", prefix + "d");

        assertThatThrownBy(() -> importer.importBuggy(file)).isInstanceOf(IllegalArgumentException.class);

        // The two rows before the bad one stayed in the database.
        assertThat(customers.countByNameStartingWith(prefix)).isEqualTo(2);
    }

    @Test
    void fixed_allOrNothing() {
        String prefix = "inc03-" + UUID.randomUUID() + "-";
        List<String> file = List.of(prefix + "a", prefix + "b", "   ", prefix + "d");

        assertThatThrownBy(() -> importer.importFixed(file)).isInstanceOf(IllegalArgumentException.class);

        assertThat(customers.countByNameStartingWith(prefix)).isZero();
    }
}
