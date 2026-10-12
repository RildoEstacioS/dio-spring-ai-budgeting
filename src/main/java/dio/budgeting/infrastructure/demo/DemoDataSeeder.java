package dio.budgeting.infrastructure.demo;

import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.input.PersistTransactionInput;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seed v1 (12 despesas), preservado como referência. Só roda no perfil "demo-v1"; o perfil "demo" usa o
 * {@link DemoDataSeederV2}. Popula apenas banco vazio e nunca apaga, substitui ou duplica dados existentes.
 */
@Component
@Profile("demo-v1")
public class DemoDataSeeder implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    static final List<PersistTransactionInput> DEMO_TRANSACTIONS = List.of(
            new PersistTransactionInput("Supermercado - compra do mês", 48790, Category.GROCERIES),
            new PersistTransactionInput("Feira livre", 6350, Category.GROCERIES),
            new PersistTransactionInput("Padaria", 2380, Category.GROCERIES),
            new PersistTransactionInput("Hortifruti", 4725, Category.GROCERIES),
            new PersistTransactionInput("Açougue", 11990, Category.GROCERIES),
            new PersistTransactionInput("Farmácia - antialérgico", 3290, Category.PHARMA),
            new PersistTransactionInput("Drogaria - vitaminas", 8999, Category.PHARMA),
            new PersistTransactionInput("Farmácia - protetor solar", 5490, Category.PHARMA),
            new PersistTransactionInput("Posto - gasolina", 25000, Category.AUTO),
            new PersistTransactionInput("Troca de óleo", 18900, Category.AUTO),
            new PersistTransactionInput("Estacionamento", 1500, Category.AUTO),
            new PersistTransactionInput("Lavagem do carro", 4000, Category.AUTO));

    private final TransactionRepository transactionRepository;
    private final PersistTransactionUseCase persistTransactionUseCase;

    public DemoDataSeeder(TransactionRepository transactionRepository,
                          PersistTransactionUseCase persistTransactionUseCase) {
        this.transactionRepository = transactionRepository;
        this.persistTransactionUseCase = persistTransactionUseCase;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    int seed() {
        if (!transactionRepository.findAll().isEmpty()) {
            log.info("Perfil demo: o banco já possui transações; nenhum dado de demonstração foi criado.");
            return 0;
        }

        DEMO_TRANSACTIONS.forEach(persistTransactionUseCase::execute);
        log.info("Perfil demo: {} transações de demonstração criadas.", DEMO_TRANSACTIONS.size());
        return DEMO_TRANSACTIONS.size();
    }
}
