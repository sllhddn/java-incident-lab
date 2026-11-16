package io.github.sllhddn.incidentlab.repo;

import io.github.sllhddn.incidentlab.domain.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    /**
     * Check and decrement in one statement. PostgreSQL re-evaluates the WHERE clause
     * after waiting for a row lock, so concurrent buyers cannot take the same unit.
     *
     * @return 1 if the stock was reserved, 0 if there was not enough
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = """
            update product
               set stock = stock - :qty, version = version + 1
             where id = :id and stock >= :qty
            """)
    int decrementStock(@Param("id") Long id, @Param("qty") int qty);

    /** Used only by the INC-04 buggy path: writes back a value computed from an earlier read. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(nativeQuery = true, value = "update product set stock = :stock where id = :id")
    int overwriteStock(@Param("id") Long id, @Param("stock") int stock);

    @Query("select p.stock from Product p where p.id = :id")
    int currentStock(@Param("id") Long id);
}
