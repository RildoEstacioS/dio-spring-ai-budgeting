package dio.budgeting.domain;

/**
 * Categorias de lançamento. GROCERIES, PHARMA e AUTO mantêm os nomes da v1 para preservar os dados existentes.
 */
public enum Category {
    SALARY(EntryType.INCOME, "Salário", "Remuneração fixa"),
    FREELANCE(EntryType.INCOME, "Freelance", "Trabalhos avulsos"),
    REFUND(EntryType.INCOME, "Reembolso", "Devoluções e estornos"),
    INVESTMENT_INCOME(EntryType.INCOME, "Rendimentos", "Juros e dividendos"),
    OTHER_INCOME(EntryType.INCOME, "Outras receitas", "Receitas que não se encaixam nas demais"),

    GROCERIES(EntryType.EXPENSE, "Mercado", "Mercado, feira e açougue"),
    FOOD(EntryType.EXPENSE, "Alimentação fora", "Restaurante, lanche e delivery"),
    PHARMA(EntryType.EXPENSE, "Farmácia", "Remédios e itens de farmácia"),
    HEALTH(EntryType.EXPENSE, "Saúde", "Consultas, exames e plano de saúde"),
    AUTO(EntryType.EXPENSE, "Automóvel", "Combustível, manutenção e estacionamento"),
    HOUSING(EntryType.EXPENSE, "Moradia", "Aluguel, condomínio e IPTU"),
    BILLS(EntryType.EXPENSE, "Contas da casa", "Luz, água, gás, internet e telefone"),
    SUBSCRIPTIONS(EntryType.EXPENSE, "Assinaturas", "Streaming, apps e serviços digitais"),
    LEISURE(EntryType.EXPENSE, "Lazer", "Passeios, cinema e viagens"),
    EDUCATION(EntryType.EXPENSE, "Educação", "Cursos, livros e mensalidades"),
    OTHER_EXPENSE(EntryType.EXPENSE, "Outras despesas", "Despesas que não se encaixam nas demais");

    private final EntryType type;
    private final String label;
    private final String description;

    Category(EntryType type, String label, String description) {
        this.type = type;
        this.label = label;
        this.description = description;
    }

    public EntryType type() {
        return type;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public boolean isCompatibleWith(EntryType entryType) {
        return type == entryType;
    }
}
