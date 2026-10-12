package dio.budgeting.infrastructure.demo;

import dio.budgeting.application.PersistTransactionUseCase;
import dio.budgeting.application.input.TransactionCommand;
import dio.budgeting.domain.Category;
import dio.budgeting.domain.EntryStatus;
import dio.budgeting.domain.EntryType;
import dio.budgeting.domain.PaymentMethod;
import dio.budgeting.domain.Recurrence;
import dio.budgeting.domain.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Seed v2 do perfil "demo": cerca de 40 receitas e despesas em três meses, com datas relativas ao Clock.
 * Só popula se o banco conectado for exatamente app.demo.database e se ele estiver vazio. Nunca apaga,
 * substitui ou duplica dados. Recorrência é apenas metadado (salário, aluguel, internet e streaming).
 */
@Component
@Profile("demo")
public class DemoDataSeederV2 implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeederV2.class);

    private final TransactionRepository transactionRepository;
    private final PersistTransactionUseCase persistTransactionUseCase;
    private final Clock clock;
    private final String expectedDatabase;
    private final CurrentDatabase currentDatabase;

    public DemoDataSeederV2(TransactionRepository transactionRepository,
                            PersistTransactionUseCase persistTransactionUseCase,
                            Clock clock,
                            @Value("${app.demo.database}") String expectedDatabase,
                            CurrentDatabase currentDatabase) {
        this.transactionRepository = transactionRepository;
        this.persistTransactionUseCase = persistTransactionUseCase;
        this.clock = clock;
        this.expectedDatabase = expectedDatabase;
        this.currentDatabase = currentDatabase;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed();
    }

    int seed() {
        var database = currentDatabase.name();
        if (database == null || expectedDatabase == null || expectedDatabase.isBlank()
                || !database.equalsIgnoreCase(expectedDatabase)) {
            log.warn("Perfil demo: seed bloqueado. O banco conectado ({}) não corresponde a app.demo.database ({}).",
                    database, expectedDatabase);
            return 0;
        }
        if (!transactionRepository.findAll().isEmpty()) {
            log.info("Perfil demo: o banco já possui lançamentos; nenhum dado de demonstração foi criado.");
            return 0;
        }

        var commands = demoTransactions(LocalDate.now(clock));
        commands.forEach(persistTransactionUseCase::create);
        log.info("Perfil demo: {} lançamentos de demonstração criados.", commands.size());
        return commands.size();
    }

    /**
     * Status nulo é derivado da data: dias ainda por vir no mês atual entram como previstos.
     */
    static List<TransactionCommand> demoTransactions(LocalDate today) {
        var current = YearMonth.from(today);
        List<YearMonth> months = List.of(current.minusMonths(2), current.minusMonths(1), current);
        List<TransactionCommand> commands = new ArrayList<>();

        long[] electricity = {18750, 21430, 16980};
        long[] supermarket = {48790, 52310, 45120};
        long[] market = {6350, 7120, 5980};
        long[] fuel = {25000, 23000, 26550};
        long[] delivery = {8740, 12590, 6890};

        for (int i = 0; i < months.size(); i++) {
            var month = months.get(i);
            commands.add(income("Salário", 650000, Category.SALARY, day(month, 5), PaymentMethod.BANK_TRANSFER, Recurrence.MONTHLY));
            commands.add(expense("Aluguel", 180000, Category.HOUSING, day(month, 10), PaymentMethod.BANK_TRANSFER, Recurrence.MONTHLY));
            commands.add(expense("Internet fibra", 11990, Category.BILLS, day(month, 15), PaymentMethod.DEBIT_CARD, Recurrence.MONTHLY));
            commands.add(expense("Streaming de vídeo", 3990, Category.SUBSCRIPTIONS, day(month, 12), PaymentMethod.CREDIT_CARD, Recurrence.MONTHLY));
            commands.add(expense("Conta de luz", electricity[i], Category.BILLS, day(month, 18), PaymentMethod.PIX, Recurrence.NONE));
            commands.add(expense("Supermercado - compra do mês", supermarket[i], Category.GROCERIES, day(month, 7), PaymentMethod.DEBIT_CARD, Recurrence.NONE));
            commands.add(expense("Feira livre", market[i], Category.GROCERIES, day(month, 21), PaymentMethod.CASH, Recurrence.NONE));
            commands.add(expense("Academia", 9990, Category.LEISURE, day(month, 3), PaymentMethod.CREDIT_CARD, Recurrence.NONE));
            commands.add(expense("Posto - gasolina", fuel[i], Category.AUTO, day(month, 14), PaymentMethod.CREDIT_CARD, Recurrence.NONE));
            commands.add(expense("Delivery", delivery[i], Category.FOOD, day(month, 24), PaymentMethod.PIX, Recurrence.NONE));
        }

        var twoMonthsAgo = months.get(0);
        commands.add(expense("Consulta médica", 25000, Category.HEALTH, day(twoMonthsAgo, 20), PaymentMethod.PIX, Recurrence.NONE));
        commands.add(expense("Farmácia - antialérgico", 3290, Category.PHARMA, day(twoMonthsAgo, 11), PaymentMethod.DEBIT_CARD, Recurrence.NONE));

        var lastMonth = months.get(1);
        commands.add(income("Freelance - site institucional", 180000, Category.FREELANCE, day(lastMonth, 16), PaymentMethod.PIX, Recurrence.NONE));
        commands.add(income("Reembolso do plano de saúde", 12000, Category.REFUND, day(lastMonth, 22), PaymentMethod.BANK_TRANSFER, Recurrence.NONE));
        commands.add(income("Rendimentos da poupança", 4512, Category.INVESTMENT_INCOME, day(lastMonth, 28), PaymentMethod.BANK_TRANSFER, Recurrence.NONE));
        commands.add(expense("Curso online de Java", 29700, Category.EDUCATION, day(lastMonth, 8), PaymentMethod.CREDIT_CARD, Recurrence.NONE));
        commands.add(expense("Cinema", 6400, Category.LEISURE, day(lastMonth, 26), PaymentMethod.CREDIT_CARD, Recurrence.NONE));

        commands.add(income("Freelance - manutenção de app", 95000, Category.FREELANCE, day(current, 20), PaymentMethod.PIX, Recurrence.NONE));
        commands.add(expense("Farmácia - vitaminas", 8999, Category.PHARMA, day(current, 2), PaymentMethod.PIX, Recurrence.NONE));
        commands.add(expense("Padaria", 2380, Category.GROCERIES, today, PaymentMethod.CASH, Recurrence.NONE));

        commands.add(planned(EntryType.EXPENSE, "Aluguel", 180000, Category.HOUSING, day(current.plusMonths(1), 10), PaymentMethod.BANK_TRANSFER, Recurrence.MONTHLY));
        commands.add(planned(EntryType.EXPENSE, "IPVA - parcela", 41500, Category.AUTO, today.minusDays(3), PaymentMethod.PIX, Recurrence.NONE));

        return commands;
    }

    private static LocalDate day(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }

    private static TransactionCommand income(String description, long cents, Category category, LocalDate date,
                                             PaymentMethod paymentMethod, Recurrence recurrence) {
        return new TransactionCommand(EntryType.INCOME, description, cents, category, date, paymentMethod, recurrence, null, null);
    }

    private static TransactionCommand expense(String description, long cents, Category category, LocalDate date,
                                              PaymentMethod paymentMethod, Recurrence recurrence) {
        return new TransactionCommand(EntryType.EXPENSE, description, cents, category, date, paymentMethod, recurrence, null, null);
    }

    private static TransactionCommand planned(EntryType type, String description, long cents, Category category,
                                              LocalDate date, PaymentMethod paymentMethod, Recurrence recurrence) {
        return new TransactionCommand(type, description, cents, category, date, paymentMethod, recurrence,
                EntryStatus.PLANNED, "Lançamento previsto de demonstração");
    }
}
