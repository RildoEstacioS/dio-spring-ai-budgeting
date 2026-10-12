// Cliente da API REST local (mesma origem). Erros chegam como ProblemDetail.

export class ApiError extends Error {
    constructor(status, problem) {
        super(problem?.detail || problem?.title || `HTTP ${status}`);
        this.status = status;
        this.problem = problem || {};
    }

    /** Erros por campo ({field, message}) enviados pela API, quando houver. */
    get fieldErrors() {
        return Array.isArray(this.problem.errors) ? this.problem.errors : [];
    }
}

export async function api(method, url, body) {
    const options = { method, headers: { Accept: 'application/json' } };
    if (body !== undefined) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(url, options);
    } catch {
        throw new ApiError(0, {
            title: 'Sem conexão com a API',
            detail: 'Verifique se a aplicação está rodando e tente novamente.',
        });
    }

    if (response.status === 204) {
        return null;
    }
    const isJson = (response.headers.get('Content-Type') || '').includes('json');
    const data = isJson ? await response.json() : null;
    if (!response.ok) {
        throw new ApiError(response.status, data);
    }
    return data;
}
