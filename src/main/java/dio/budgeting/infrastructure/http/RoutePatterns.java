package dio.budgeting.infrastructure.http;

/**
 * Padrões de rota que separam GET /transactions/{id} de GET /transactions/{category}.
 * Precisam ser constantes de compilação para uso em anotações.
 */
final class RoutePatterns {
    private static final String HEX = "[0-9a-fA-F]";
    private static final String HEX4 = HEX + HEX + HEX + HEX;

    /**
     * Somente UUID completo (8-4-4-4-12).
     */
    static final String UUID = HEX4 + HEX4 + "-" + HEX4 + "-" + HEX4 + "-" + HEX4 + "-" + HEX4 + HEX4 + HEX4;

    /**
     * Somente categorias reais do enum Category. Um teste garante que a lista acompanha o enum.
     */
    static final String CATEGORY = "SALARY|FREELANCE|REFUND|INVESTMENT_INCOME|OTHER_INCOME"
            + "|GROCERIES|FOOD|PHARMA|HEALTH|AUTO|HOUSING|BILLS|SUBSCRIPTIONS|LEISURE|EDUCATION|OTHER_EXPENSE";

    private RoutePatterns() {
    }
}
