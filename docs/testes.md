Este arquivo explica os cenários cobertos pelos testes, os resultados registrados e as validações que ainda faltam.

Testes do Pediu Patrão

Evidências de testes

Comando: .\mvnw.cmd -B verify. Resultado: 163 testes, zero falhas, zero erros, zero ignorados, BUILD SUCCESS. JAR gerado: target/pediupatrao-1.0-SNAPSHOT.jar. node --check src/main/resources/static/js/dashboard.js passou.

DataInitializerTest: 2 casos, admin com BCrypt e preservação da conta existente.
DashboardControllerTest: 9 casos, sessão, perfis, filtros, datas e erro 503 de banco.
PermissoesRotasTest: 40 casos, perfis, páginas/APIs, bloqueios, CSRF, desconto, cancelamento e contas.
ClienteServiceTest: 5 casos, auditoria de inclusão/alteração/exclusão, ausente e falha antes da exclusão.
DashboardServiceTest: 8 casos, indicadores, ranking, cancelamentos, períodos, query e limites.
PedidoServiceTest: 16 casos, cálculo, catálogo, saída, cancelamento, desconto, validação e histórico.
PedidoTransicoesTest: 50 casos, 49 pares entre sete status e ausência de status; seis transições normais permitidas.
ProdutoServiceTest: 18 casos, campos, tipos, preço inválido/zero, ausente e ativo.
SegurancaServicosTest: 11 casos, autorização direta, auditoria, funcionários e edição de conta.
UsuarioDetailsServiceTest: 4 casos, três perfis com hash preservado e usuário ausente.

Os casos parametrizados são contados por execução. O resumo preservado está em docs/evidencias/validacao-atual.md. Cópias dos dez relatórios individuais desta execução: docs/evidencias/atual. Originais: target/surefire-reports, arquivos com.umc.pediupatrao.service.NomeDaClasseTest.txt, com.umc.pediupatrao.controller.NomeDaClasseTest.txt e com.umc.pediupatrao.config.DataInitializerTest.txt. XMLs: target/surefire-reports/TEST-*.xml. Log local: target/revisao-testes.log.

Os testes adicionais reproduziram aceitação indevida de NaN e infinito positivo em ProdutoService. A validação foi corrigida para preço finito e a suíte passou após a correção.

MockMvc usa os filtros reais de segurança; serviços de rota são simulados nas suítes correspondentes. Testes de serviços usam repositórios/MongoTemplate simulados. SegurancaServicosTest usa proxies reais de autorização. Nenhum desses resultados comprova conexão ou escrita no Atlas.

Capturas históricas: docs/evidencias/dashboard-desktop.png, dashboard-cancelados.png, dashboard-vazio.png e dashboard-mobile.png. Mostram a prévia com dados fictícios. O registro anterior informa 42 pedidos, 36 válidos, seis cancelados, faturamento R$ 2.946,60, ticket R$ 81,85 e trinta em andamento. Inclui filtro de cancelados, período vazio e tela pequena. Não foram refeitas na execução dos 163 testes e não mostram o Atlas.

A pasta docs/evidencias/atual contém os dez relatórios da suíte completa de 163 testes. A versão antiga de 84 testes foi removida para evitar duplicação. .github/workflows/testes.yml executa mvn verify e node --check em push/PR; não foi feito push nem confirmada execução remota.

Limites e pendências

O dashboard está implementado e tem testes de cálculo, API e autorização, além das capturas históricas. Ainda não há evidência nesta entrega de operação completa contra MongoDB real. Não foram executados testes de concorrência, atomicidade de auditoria, carga, acessibilidade completa, ViaCEP, atualização automática, perda de rede e expiração de sessão no navegador.

Faltam fluxos automatizados completos de navegador dos cadastros e pedidos, matriz HTTP de todos os campos e autorização direta de todos os métodos dos serviços. Não há medição de cobertura por linhas/ramificações. A aprovação de 163 casos não significa cobertura de todos os cenários.

Homologação pendente em banco separado: cadastrar cliente/produto/entregador; criar pedidos com/sem desconto; editar em RECEBIDO; percorrer entrega e retirada; finalizar; cancelar antes da saída; verificar bloqueios posteriores; conferir pedidos/auditorias persistidos; comparar resumo com documentos e limites de dia. Depois exercitar rede, sessão e atualização automática. Esses passos não devem ser apresentados como testes já executados.

Verificação HTTP do dashboard: login com CSRF e sessão, página com resposta 200, resumo de 42 pedidos, seis cancelados com vendas zeradas, catálogo de filtros e período vazio sem pedidos. Dados fictícios da DashboardPreview; não foi usado o Atlas. Resultado completo: docs/evidencias/dashboard-http-atual.md.
