package dio.budgeting.infrastructure.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * No MySQL, o catálogo da conexão é o banco selecionado na URL JDBC.
 */
@Component
@Profile("demo")
public class JdbcCurrentDatabase implements CurrentDatabase {
    private final DataSource dataSource;

    public JdbcCurrentDatabase(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public String name() {
        try (var connection = dataSource.getConnection()) {
            return connection.getCatalog();
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível identificar o banco conectado.", e);
        }
    }
}
