package dio.budgeting.infrastructure.demo;

/**
 * Nome do banco ao qual a aplicação está conectada.
 */
@FunctionalInterface
public interface CurrentDatabase {
    String name();
}
