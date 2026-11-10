Endpoints do Pediu Patrão

Inclui APIs, autenticação, páginas e formulários. Exemplos usam dados fictícios.

Convenções de acesso e erros

Base local: http://localhost:8080. APIs usam /api e JSON. Envie Accept: application/json e, quando houver corpo JSON, Content-Type: application/json. IDs são strings. No PUT, o ID da rota prevalece sobre o corpo. Só o dashboard possui os filtros documentados; as outras listagens não aceitam paginação ou filtros de negócio.

GET /login retorna HTML público com username, password e _csrf. Mantenha os cookies dessa sessão e envie POST /login em application/x-www-form-urlencoded. Exemplo: username=admin&password=teste123&_csrf=TOKEN. Sucesso retorna 302 para /; credenciais inválidas retornam 302 para /login?error. A senha inicial não é redefinida em contas existentes.

Após login, envie Cookie: JSESSIONID=SESSAO. Escritas POST, PUT e DELETE exigem X-CSRF-TOKEN: TOKEN da mesma sessão. O token atual está nas metas _csrf e _csrf_header das páginas renderizadas. Os exemplos de escrita pressupõem esses cabeçalhos. Os exemplos JSON não são executáveis anonimamente.

POST /logout recebe o token CSRF, invalida a sessão e retorna 302 para /login?logout. Não recebe corpo JSON.

Erros comuns: 401 sem sessão válida; 403 por perfil sem permissão ou CSRF ausente/inválido; 400 por JSON malformado, parâmetro obrigatório ausente ou tipo incompatível; 405 por método não mapeado quando a requisição passa pela segurança. A segurança pode negar uma rota antes de verificar seu mapeamento. Falhas não tratadas podem retornar 500. No dashboard, DataAccessException é tratada como 503.

Exemplo 401: {"message":"Sessão expirada. Entre novamente."}
Exemplo 403: {"message":"Acesso negado para este perfil."}
Erros de regra tratados: {"message":"Descrição do erro."}

Não há envelope único para todos os erros: falhas de parâmetros e desserialização tratadas pelo Spring podem ter outro corpo; um 404 explícito de usuário pode vir vazio. Os erros específicos abaixo se somam aos comuns.

Os exemplos de resposta mostram campos relevantes. Opcionais nulos e campos antigos podem aparecer adicionalmente. Respostas de pedidos mantêm seus demais campos nas mudanças de status, cancelamento e desconto. Usuários nunca retornam password.

Usuários

Entrada: username, password, role. Perfis aceitos: ADMIN, GERENTE, ATENDENTE. Criação exige senha não vazia. Edição por ADMIN exige role. Na edição própria, omita role ou mantenha o atual; password vazio/ausente preserva a senha.

GET /api/usuarios. ADMIN. Sem parâmetros ou corpo.
Requisição: GET /api/usuarios
Resposta 200: [{"id":"usuario-1","username":"ana","role":"ATENDENTE"}]
Erros: comuns, principalmente 401/403.

GET /api/usuarios/{id}. ADMIN. id na rota.
Requisição: GET /api/usuarios/usuario-1
Resposta 200: {"id":"usuario-1","username":"ana","role":"ATENDENTE"}
Erro específico: 404 sem corpo se ausente.

POST /api/usuarios. ADMIN. Corpo de usuário novo.
Requisição: POST /api/usuarios
Corpo: {"username":"ana","password":"SenhaDeTeste123","role":"ATENDENTE"}
Resposta 201: {"id":"usuario-1","username":"ana","role":"ATENDENTE"}
Erro específico: 400 {"message":"Não foi possível salvar o usuário."} para senha/perfil ausente ou falha tratada de persistência. Não se pressupõe validação completa de username ou unicidade no serviço.

PUT /api/usuarios/{id}. ADMIN ou proprietário. id e corpo de edição.
Requisição: PUT /api/usuarios/usuario-1
Corpo de edição própria: {"username":"ana","password":""}
Resposta 200: {"id":"usuario-1","username":"ana","role":"ATENDENTE"}
Erros: 403 para conta alheia sem ADMIN; 400 {"message":"Não foi possível atualizar o usuário."} para alteração de perfil proibida, perfil ausente na edição por ADMIN, usuário ausente ou falha tratada de persistência.

DELETE /api/usuarios/{id}. ADMIN. id na rota, sem corpo.
Requisição: DELETE /api/usuarios/usuario-1
Resposta 204 sem corpo.
Erros: 404 sem corpo para ausente; falha tratada de exclusão pode retornar 400 com message.

Clientes

Campos: nome, telefone, cep, logradouro, numero, complemento, bairro, cidade, estado e endereco legado. Não há validação completa de formato e obrigatoriedade no backend. Nos exemplos, use o cadastro completo recomendado. Na criação, omita id.

GET /api/clientes. Os três perfis. Sem parâmetros ou corpo.
Requisição: GET /api/clientes
Resposta 200: [{"id":"cliente-1","nome":"Ana","telefone":"11999990000","cep":"01001000","logradouro":"Praça da Sé","numero":"10","complemento":"Sala 2","bairro":"Sé","cidade":"São Paulo","estado":"SP","endereco":null}]
Erros: comuns e falhas de infraestrutura não tratadas.

POST /api/clientes. Os três perfis. Corpo de cliente novo.
Requisição: POST /api/clientes
Corpo: {"nome":"Ana","telefone":"11999990000","cep":"01001000","logradouro":"Praça da Sé","numero":"10","complemento":"Sala 2","bairro":"Sé","cidade":"São Paulo","estado":"SP"}
Resposta 201: o cadastro enviado com id gerado, por exemplo {"id":"cliente-1","nome":"Ana","telefone":"11999990000","cep":"01001000","logradouro":"Praça da Sé","numero":"10","complemento":"Sala 2","bairro":"Sé","cidade":"São Paulo","estado":"SP","endereco":null}.
Erro específico: 400 se enviar id de atualização inexistente. O serviço compartilhado considera id preenchido uma edição.

PUT /api/clientes/{id}. Os três perfis. id e corpo completo; omissões não são preservadas automaticamente.
Requisição: PUT /api/clientes/cliente-1
Corpo: {"nome":"Ana","telefone":"11888880000","cep":"01001000","logradouro":"Praça da Sé","numero":"10","complemento":"Sala 2","bairro":"Sé","cidade":"São Paulo","estado":"SP"}
Resposta 200: {"id":"cliente-1","nome":"Ana","telefone":"11888880000","cep":"01001000","logradouro":"Praça da Sé","numero":"10","complemento":"Sala 2","bairro":"Sé","cidade":"São Paulo","estado":"SP","endereco":null}
Erro: 400 {"message":"Cliente não encontrado para atualização."}.

DELETE /api/clientes/{id}. ADMIN e GERENTE. id, sem corpo.
Requisição: DELETE /api/clientes/cliente-1
Resposta 204 sem corpo, com auditoria de exclusão.
Erro: 400 {"message":"Cliente não encontrado."}.

Produtos

Campos: nome, tipo, tipoPizza, sabor, preco, ativo. Tipos: Pizza, Lanche, Bebida, Opcional. Pizza exige tipoPizza/sabor. Nome/tipo não podem ser vazios. Preço precisa ser informado, finito e não negativo. ativo ausente vira false. Na criação, omita id.

GET /api/produtos. Os três perfis. Sem parâmetros ou corpo.
Requisição: GET /api/produtos
Resposta 200: [{"id":"produto-1","nome":"Calabresa","tipo":"Pizza","tipoPizza":"Grande","sabor":"Calabresa","preco":25.0,"ativo":true}]
Erros: comuns.

POST /api/produtos. ADMIN e GERENTE. Corpo de produto novo.
Requisição: POST /api/produtos
Corpo: {"nome":"Calabresa","tipo":"Pizza","tipoPizza":"Grande","sabor":"Calabresa","preco":25.0,"ativo":true}
Resposta 201: {"id":"produto-1","nome":"Calabresa","tipo":"Pizza","tipoPizza":"Grande","sabor":"Calabresa","preco":25.0,"ativo":true}
Erros: 400 para nome, tipo, campos de pizza ou preço inválidos. Exemplos: {"message":"O nome do produto é obrigatório."}, {"message":"O tipo deve ser Pizza, Lanche, Bebida ou Opcional."}, {"message":"O preço deve ser informado e não pode ser negativo."}. A mensagem de preço também é usada para valores não finitos.

PUT /api/produtos/{id}. ADMIN e GERENTE. id e corpo completo.
Requisição: PUT /api/produtos/produto-1
Corpo: {"nome":"Calabresa","tipo":"Pizza","tipoPizza":"Grande","sabor":"Calabresa","preco":30.0,"ativo":true}
Resposta 200: {"id":"produto-1","nome":"Calabresa","tipo":"Pizza","tipoPizza":"Grande","sabor":"Calabresa","preco":30.0,"ativo":true}
Erros: validações da criação; 400 {"message":"Produto não encontrado para atualização."}.

DELETE /api/produtos/{id}. ADMIN e GERENTE. id, sem corpo.
Requisição: DELETE /api/produtos/produto-1
Resposta 204 sem corpo. Não há consulta prévia nem 404 específico para ausente. Itens históricos preservam descrição e preço.
Erros: comuns e possíveis falhas de infraestrutura.

Entregadores

Campos: nome, telefone, disponivel, com padrão true se ausente. Não há validação completa de cadastro ou consulta de existência antes do PUT. A saída de pedido exige entregador existente com nome preenchido. Na criação, omita id.

GET /api/entregadores. Os três perfis. Sem parâmetros ou corpo.
Requisição: GET /api/entregadores
Resposta 200: [{"id":"entregador-1","nome":"João","telefone":"11999991111","disponivel":true}]
Erros: comuns.

POST /api/entregadores. ADMIN e GERENTE. Corpo de novo entregador.
Requisição: POST /api/entregadores
Corpo: {"nome":"João","telefone":"11999991111","disponivel":true}
Resposta 200: {"id":"entregador-1","nome":"João","telefone":"11999991111","disponivel":true}
Erros: comuns. Essa criação retorna 200, não 201.

PUT /api/entregadores/{id}. ADMIN e GERENTE. id e corpo completo.
Requisição: PUT /api/entregadores/entregador-1
Corpo: {"nome":"João","telefone":"11888881111","disponivel":false}
Resposta 200: {"id":"entregador-1","nome":"João","telefone":"11888881111","disponivel":false}
Erros: comuns. Não há 404 implementado; o repositório pode criar documento com o ID informado se ele não existir.

DELETE /api/entregadores/{id}. ADMIN e GERENTE. id, sem corpo.
Requisição: DELETE /api/entregadores/entregador-1
Resposta 204 sem corpo. Não há consulta prévia nem 404 específico.
Erros: comuns e possíveis falhas de infraestrutura.

Pedidos

Entrada principal: clienteId, itens, percentualDesconto opcional. Item: produtoId e quantidade inteira positiva. descricao e precoUnitario são preenchidos pelo servidor. O servidor gera ID, número, status, total, responsável e entrada. tipoPizza, quantidade e sabor no nível principal são legados; o cálculo usa itens.

GET /api/pedidos. Os três perfis. Sem parâmetros ou corpo.
Requisição: GET /api/pedidos
Resposta 200: [{"id":"pedido-1","numeroPedido":1,"clienteId":"cliente-1","status":"RECEBIDO","itens":[{"produtoId":"produto-1","descricao":"Calabresa","quantidade":2,"precoUnitario":25.00}],"percentualDesconto":0,"valorTotal":50.00,"responsavelRegistro":"ana","dataHoraEntrada":"2026-11-10T18:30:00"}]
Erros: comuns. Não recebe os filtros do dashboard.

POST /api/pedidos. GERENTE e ATENDENTE. Corpo com clienteId e itens. Desconto positivo só por GERENTE.
Requisição: POST /api/pedidos
Corpo: {"clienteId":"cliente-1","itens":[{"produtoId":"produto-1","quantidade":2}],"percentualDesconto":0}
Resposta 200: {"id":"pedido-1","numeroPedido":1,"clienteId":"cliente-1","status":"RECEBIDO","itens":[{"produtoId":"produto-1","descricao":"Calabresa","quantidade":2,"precoUnitario":25.00}],"percentualDesconto":0,"valorTotal":50.00,"responsavelRegistro":"ana","dataHoraEntrada":"2026-11-10T18:30:00"}
Erros: 400 para cliente inexistente, lista vazia, item/produto ausente, quantidade não positiva, preço inválido ou percentual fora de zero a 20; 403 se ATENDENTE aplicar desconto positivo. Exemplos: {"message":"Selecione um cliente cadastrado."}, {"message":"O pedido deve possuir ao menos um item."}, {"message":"A quantidade de cada item deve ser maior que zero."}.

PUT /api/pedidos/{id}. GERENTE e ATENDENTE. id, clienteId e itens completos. Só em RECEBIDO. ATENDENTE pode omitir desconto para preservar o atual. Para GERENTE, omissão é tratada como zero.
Requisição: PUT /api/pedidos/pedido-1
Corpo: {"clienteId":"cliente-1","itens":[{"produtoId":"produto-1","quantidade":3}],"percentualDesconto":0}
Resposta 200: {"id":"pedido-1","numeroPedido":1,"clienteId":"cliente-1","status":"RECEBIDO","itens":[{"produtoId":"produto-1","descricao":"Calabresa","quantidade":3,"precoUnitario":25.00}],"percentualDesconto":0,"valorTotal":75.00,"responsavelRegistro":"ana","dataHoraEntrada":"2026-11-10T18:30:00"}
Erros: validações da criação; 404 {"message":"Pedido não encontrado."}; 400 {"message":"Somente pedidos recebidos podem ser editados."}; 403 se ATENDENTE mudar desconto.

PUT /api/pedidos/{id}/status. GERENTE e ATENDENTE. id na rota, status obrigatório e entregadorId na query. Sem corpo. entregadorId é obrigatório para SAIU_PARA_ENTREGA e RETIRADO.
Requisição: PUT /api/pedidos/pedido-1/status?status=EM_PREPARACAO
Resposta 200: {"id":"pedido-1","status":"EM_PREPARACAO","valorTotal":50.00}
Exemplo de saída a partir de PRONTO: PUT /api/pedidos/pedido-1/status?status=SAIU_PARA_ENTREGA&entregadorId=entregador-1
Resposta 200: {"id":"pedido-1","status":"SAIU_PARA_ENTREGA","valorTotal":50.00,"entregadorSaidaId":"entregador-1","entregadorSaidaNome":"João","entregadorSaidaTelefone":"11999991111","responsavelSaida":"João","dataHoraSaida":"2026-11-10T19:00:00"}
Erros: 404 para pedido ausente; 400 para status desconhecido, transição inválida, entregador ausente, inexistente ou sem nome. Exemplos: {"message":"Transição de status inválida."}, {"message":"Selecione o entregador responsável pela saída."}.

PUT /api/pedidos/{id}/cancelar. GERENTE. id na rota, justificativa obrigatória na query. Sem corpo.
Requisição: PUT /api/pedidos/pedido-1/cancelar?justificativa=Cliente%20desistiu
Resposta 200: {"id":"pedido-1","status":"CANCELADO","valorTotal":50.00,"justificativaCancelamento":"Cliente desistiu"}
Erros: 404 para pedido ausente; 400 para justificativa vazia ou cancelamento após saída/retirada ou término. Exemplos: {"message":"A justificativa do cancelamento é obrigatória."}, {"message":"O pedido só pode ser cancelado antes da saída para entrega ou retirada."}; 403 para ADMIN/ATENDENTE.

PUT /api/pedidos/{id}/desconto. GERENTE. id na rota e percentual decimal obrigatório na query entre zero e 20. Sem corpo.
Requisição: PUT /api/pedidos/pedido-1/desconto?percentual=10
Resposta 200: {"id":"pedido-1","status":"RECEBIDO","percentualDesconto":10,"valorTotal":45.00}
Erros: 404 para ausente; 400 para percentual inválido ou etapa posterior à saída. Exemplos: {"message":"O desconto deve estar entre 0% e 20%."}, {"message":"Descontos só podem ser alterados antes da saída do pedido."}; 403 para ADMIN/ATENDENTE.

DELETE /api/pedidos/{id}. GERENTE admitido pela rota. id, sem corpo.
Requisição: DELETE /api/pedidos/pedido-1
Resposta sempre 400: {"message":"Pedidos devem ser cancelados com justificativa; o histórico é preservado."}
Não existe exclusão bem-sucedida. O serviço rejeita inclusive ID inexistente. ADMIN e ATENDENTE recebem 403.

Auditoria

GET /api/auditorias. ADMIN e GERENTE. Sem parâmetros ou corpo. Sem filtros/paginação.
Requisição: GET /api/auditorias
Resposta 200: [{"id":"auditoria-1","usuario":"gerente","perfil":"GERENTE","dataHora":"2026-11-10T18:40:00","operacao":"DESCONTO","entidade":"PEDIDO","entidadeId":"pedido-1","valoresAnteriores":{"percentualDesconto":"0","valorTotal":"50.00"},"valoresNovos":{"percentualDesconto":"10","valorTotal":"45.00"},"justificativa":null}]
Erros: comuns; ATENDENTE recebe 403. Não existem operações de escrita. O exemplo é ilustrativo; os mapas contêm as diferenças detectadas.

Dashboard

GET /api/dashboard/filtros. ADMIN e GERENTE. Sem parâmetros ou corpo.
Requisição: GET /api/dashboard/filtros
Resposta 200: {"situacoes":["RECEBIDO","EM_PREPARACAO","PRONTO","SAIU_PARA_ENTREGA","RETIRADO","FINALIZADO","CANCELADO"],"agrupamentos":["dia","mes"],"maximoDias":366}
Erros: 401/403 conforme sessão e perfil.

GET /api/dashboard/resumo. ADMIN e GERENTE.
Query: inicio e fim opcionais em AAAA-MM-DD; status opcional, vazio significa todos; agrupamento opcional, dia por padrão ou mes. Status aceita minúsculas. Sem datas, consulta do primeiro dia do mês até hoje no fuso configurado do sistema. Intervalo de um a 366 dias inclusivos.
Requisição: GET /api/dashboard/resumo?inicio=2026-11-10&fim=2026-11-10&status=FINALIZADO&agrupamento=dia
Resposta 200 para duas pizzas de R$ 25 com desconto de 10%:
{"inicio":"2026-11-10","fim":"2026-11-10","agrupamento":"dia","status":"FINALIZADO","atualizadoEm":"2026-11-10T20:00:00","indicadores":{"pedidos":1,"vendas":1,"cancelados":0,"emAndamento":0,"faturamento":45.00,"ticketMedio":45.00},"vendas":[{"periodo":"2026-11-10","pedidos":1,"faturamento":45.00}],"situacoes":[{"status":"RECEBIDO","quantidade":0},{"status":"EM_PREPARACAO","quantidade":0},{"status":"PRONTO","quantidade":0},{"status":"SAIU_PARA_ENTREGA","quantidade":0},{"status":"RETIRADO","quantidade":0},{"status":"FINALIZADO","quantidade":1},{"status":"CANCELADO","quantidade":0}],"produtos":[{"produtoId":"produto-1","descricao":"Calabresa","quantidade":2,"valorBruto":50.00}],"pedidosRecentes":[{"id":"pedido-1","numeroPedido":1,"entrada":"2026-11-10T18:30:00","status":"FINALIZADO","valorTotal":45.00}]}

Sem resultados, retorna 200 com indicadores/situações zero, série preenchida com zero e listas vazias. Agrupamento mensal usa periodo no formato AAAA-MM.

Erros: 400 {"message":"Datas devem usar o formato AAAA-MM-DD."}; 400 {"message":"Informe um período de 1 a 366 dias, com início anterior ou igual ao fim."}; 400 {"message":"Agrupamento deve ser dia ou mes."}; 400 para status desconhecido; 503 {"message":"Dados temporariamente indisponíveis. Tente atualizar novamente."}. Também há 401/403.

Páginas e formulários

Páginas respondem HTML, não JSON. GET sem sessão redireciona para login; perfil insuficiente recebe 403. Formulários usam application/x-www-form-urlencoded e _csrf. Os campos são os dos cadastros acima.

GET /login, POST /login e POST /logout: parâmetros, respostas e exemplos em Convenções de acesso e erros.
GET /: início para os três perfis. GET /pedidos: lista para os três, com escritas limitadas pela API.
GET /dashboard: ADMIN e GERENTE, painel descrito em Dashboard.
GET /auditorias: ADMIN e GERENTE, histórico.
GET /funcionarios: ADMIN e GERENTE, lista de GERENTE e ATENDENTE.
GET /configuracoes: ADMIN e GERENTE, página sem API de alteração.
GET /admin: ADMIN, template administrativo.
Exemplo de leitura: GET /dashboard retorna 200, Content-Type: text/html, com o layout e a seção Visão da operação. Os outros GETs autorizados retornam HTML das respectivas páginas.

GET /usuarios: lista para ADMIN. GET /usuarios/novo: formulário para ADMIN. GET /usuarios/editar/{id}: edição para ADMIN, id na rota; ausente retorna 302 para /usuarios com mensagem.
POST /usuarios/salvar: username, password, role e _csrf; ADMIN; 302 para /usuarios.
POST /usuarios/editar/{id}: id na rota, username, password, role e _csrf; ADMIN; 302 para /usuarios.
POST /usuarios/deletar/{id}: id e _csrf; ADMIN; 302 para /usuarios.
Exemplo: POST /usuarios/salvar com username=ana&password=SenhaDeTeste123&role=ATENDENTE&_csrf=TOKEN. Resposta: 302, Location: /usuarios.

GET /clientes: lista para os três. GET /clientes/novo: formulário para os três. GET /clientes/editar/{id}: edição para os três, id na rota; ausente retorna 302 para /clientes com mensagem.
POST /clientes/salvar: campos de cliente e _csrf; os três perfis; 302 para /clientes.
POST /clientes/editar/{id}: id, campos completos de cliente e _csrf; os três; 302 para /clientes. Não há POST de exclusão de cliente no controlador; usa-se a API.
Exemplo: POST /clientes/salvar com nome=Ana&telefone=11999990000&cep=01001000&logradouro=Praca%20da%20Se&numero=10&bairro=Se&cidade=Sao%20Paulo&estado=SP&_csrf=TOKEN. Resposta: 302, Location: /clientes.

GET /produtos: catálogo para os três. GET /produtos/editar/{id}: ADMIN e GERENTE, id na rota; ausente retorna 302 para /produtos com mensagem.
POST /produtos/salvar: campos de produto, id opcional na edição e _csrf; ADMIN e GERENTE; 302 para /produtos.
POST /produtos/deletar/{id}: id e _csrf; ADMIN e GERENTE; exclui se encontrado e retorna 302 para /produtos com mensagem de sucesso ou ausência.
Exemplo: POST /produtos/salvar com nome=Calabresa&tipo=Pizza&tipoPizza=Grande&sabor=Calabresa&preco=25.00&ativo=true&_csrf=TOKEN. Resposta: 302, Location: /produtos.

GET /entregadores: manutenção para ADMIN e GERENTE.
POST /entregadores/salvar: nome, telefone, disponivel, id opcional e _csrf; ADMIN e GERENTE; 302 para /entregadores.
POST /entregadores/deletar/{id}: id e _csrf; ADMIN e GERENTE; 302 para /entregadores.
Exemplo: POST /entregadores/salvar com nome=Joao&telefone=11999991111&disponivel=true&_csrf=TOKEN. Resposta: 302, Location: /entregadores.

Erros dessas páginas: 403 por permissão/CSRF. Os GETs de edição citados redirecionam se ausentes. ErroApiHandler é restrito a REST; erros não capturados nos formulários podem produzir resposta de erro do servidor em vez de mensagem de campo. Não se aplica automaticamente o contrato JSON da API.
