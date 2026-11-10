Este arquivo registra a verificação do login, da página e das consultas do dashboard com dados fictícios.

DashboardPreview com repositório simulado, porta 8081.
GET /login: 200 e token CSRF presente.
POST /login: autenticação com sessão bem-sucedida.
GET /dashboard: 200 e HTML Visão da operação presente.
Resumo: {"pedidos":42,"vendas":36,"cancelados":6,"emAndamento":30,"faturamento":2946.6,"ticketMedio":81.85}
Cancelados: {"pedidos":6,"vendas":0,"cancelados":6,"emAndamento":0,"faturamento":0.0,"ticketMedio":0.0}
Período vazio: {"pedidos":0,"vendas":0,"cancelados":0,"emAndamento":0,"faturamento":0.0,"ticketMedio":0.0}
Filtros: {"situacoes":["RECEBIDO","EM_PREPARACAO","PRONTO","SAIU_PARA_ENTREGA","RETIRADO","FINALIZADO","CANCELADO"],"agrupamentos":["dia","mes"],"maximoDias":366}
Sem conexão com Atlas e sem nova inspeção visual no navegador.
