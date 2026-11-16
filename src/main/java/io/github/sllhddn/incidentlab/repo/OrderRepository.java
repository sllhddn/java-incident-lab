package io.github.sllhddn.incidentlab.repo;

import io.github.sllhddn.incidentlab.domain.CustomerOrder;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /** INC-02 fix: one query brings back orders, customers, lines and products. */
    @EntityGraph(attributePaths = {"customer", "lines", "lines.product"})
    @Query("select o from CustomerOrder o order by o.id")
    List<CustomerOrder> findAllWithDetails();
}
