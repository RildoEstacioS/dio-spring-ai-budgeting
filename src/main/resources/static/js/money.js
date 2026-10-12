// Conversões monetárias sem ponto flutuante.
// Todo cálculo é feito em centavos inteiros (BigInt); números só aparecem na borda com a API.

const BRL_INPUT = /^(?:\d{1,3}(?:\.\d{3})+|\d+)(?:,\d{1,2})?$/;
const API_DECIMAL = /^(-?)(\d+)(?:\.(\d{1,2}))?$/;
const integerFormatter = new Intl.NumberFormat('pt-BR');

/**
 * Converte um valor digitado em formato brasileiro ("80", "80,5", "1.234,56", "R$ 80,00") para centavos.
 * Retorna null quando o texto não está no formato esperado.
 */
export function parseBrlToCents(text) {
    const normalized = String(text ?? '').trim().replace(/^R\$\s*/i, '');
    if (!BRL_INPUT.test(normalized)) {
        return null;
    }
    const [integerPart, decimalPart = ''] = normalized.split(',');
    return BigInt(integerPart.replaceAll('.', '')) * 100n + BigInt(decimalPart.padEnd(2, '0'));
}

/**
 * Converte o amount em reais devolvido pela API (ex.: 80.00, lido pelo JSON como 80) para centavos.
 * String(número) produz a representação decimal exata mais curta ("80", "19.99"), que é lida como texto,
 * sem multiplicar por 100 em ponto flutuante.
 */
export function apiAmountToCents(value) {
    const match = API_DECIMAL.exec(String(value));
    if (!match) {
        throw new Error(`valor monetário inválido recebido da API: ${value}`);
    }
    const [, sign, integerPart, decimalPart = ''] = match;
    const cents = BigInt(integerPart) * 100n + BigInt(decimalPart.padEnd(2, '0'));
    return sign ? -cents : cents;
}

/** Formata centavos como moeda brasileira: 123456n → "R$ 1.234,56". */
export function formatCentsAsBrl(cents) {
    const negative = cents < 0n;
    const absolute = negative ? -cents : cents;
    const reais = integerFormatter.format(absolute / 100n);
    const centavos = (absolute % 100n).toString().padStart(2, '0');
    return `${negative ? '-' : ''}R$ ${reais},${centavos}`;
}

/** Converte centavos (BigInt) para o número inteiro enviado no JSON, garantindo representação exata. */
export function centsToJsonNumber(cents) {
    if (cents > BigInt(Number.MAX_SAFE_INTEGER) || cents < BigInt(Number.MIN_SAFE_INTEGER)) {
        throw new Error('valor fora do intervalo suportado');
    }
    return Number(cents);
}
