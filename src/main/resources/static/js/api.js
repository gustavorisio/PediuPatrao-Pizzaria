function requisicaoApi(url, opcoes = {}) {
    const cabecalhos = new Headers(opcoes.headers);
    const metodo = (opcoes.method || 'GET').toUpperCase();
    // Envia o token da página nas operações que alteram dados.
    if (!['GET', 'HEAD', 'OPTIONS'].includes(metodo)) {
        cabecalhos.set(document.querySelector('meta[name="_csrf_header"]').content,
            document.querySelector('meta[name="_csrf"]').content);
    }
    return fetch(url, {...opcoes, headers: cabecalhos});
}
