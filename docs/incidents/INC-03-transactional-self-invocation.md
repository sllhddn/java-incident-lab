# INC-03: "All-or-nothing" import left half the rows behind (@Transactional self-invocation)

| | |
|---|---|
| **Severity** | Sev 3: data quality; duplicates after re-run |
| **Detected by** | Ops re-ran a failed nightly import; reports showed duplicate customers |
| **Code** | [`CustomerImportService`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc03_selfinvocation/CustomerImportService.java), [`AtomicCustomerWriter`](../../src/main/java/io/github/sllhddn/incidentlab/incidents/inc03_selfinvocation/AtomicCustomerWriter.java) |
| **Test** | [`Inc03SelfInvocationTest`](../../src/test/java/io/github/sllhddn/incidentlab/incidents/Inc03SelfInvocationTest.java) |

## Symptom

Row 3 of the file had a blank name, the job failed with `IllegalArgumentException`, and the log said
"import rolled back". Rows 1 and 2 were in the table anyway. After the file was fixed and re-run, rows
1 and 2 existed twice.

## Investigation

1. The method that loops over rows is annotated `@Transactional`, so a rollback was expected.
2. Check whether a transaction actually exists: set
   `logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG`. For the import there is no
   `Creating new transaction ... saveAll` line, only one per `repository.save` call.
3. Breakpoint inside `saveAll` and look at the call stack: there is **no** `TransactionInterceptor`
   frame between `importBuggy` and `saveAll`. The call went straight from the object to itself.
   (Alternatively: `TransactionSynchronizationManager.isActualTransactionActive()` in the debugger → `false`.)

## Root cause

Spring applies `@Transactional` with a proxy around the bean. Callers from *outside* go through the proxy.
`this.saveAll(...)` is a plain Java call on the target object, so the proxy (and the transaction) is
skipped. Each `repository.save` then runs in its own short transaction and commits immediately.

## Fix

Move the transactional method to a separate bean so the call crosses the proxy:

```java
public void importFixed(List<String> names) {
    atomicWriter.saveAll(names);   // AtomicCustomerWriter, @Transactional
}
```

Other valid options: put `@Transactional` on the public entry point, or use `TransactionTemplate`.

## Prevention

- Rule of thumb in reviews: `@Transactional` on a method only called via `this` is a smell.
- Integration tests that assert rollback (count rows after a failure), not just the happy path.
- Make imports idempotent (natural key + upsert) so a re-run cannot duplicate.

## Interview angle

A favourite Spring question. Explain proxies, why self-invocation bypasses them, and the same trap for
`@Async`, `@Cacheable` and `@Retryable`.
