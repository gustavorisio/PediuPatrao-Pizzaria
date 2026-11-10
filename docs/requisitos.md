Requisitos do Pediu Patrão

Requisitos funcionais e não funcionais de todo o sistema.

1. Escopo

O sistema mantém usuários, clientes, produtos e entregadores, registra pedidos e suas etapas, calcula descontos, guarda auditoria e apresenta um dashboard administrativo. Usa Java 21, Spring Boot 3.4.4, Spring Security, Spring Data MongoDB, Thymeleaf, JavaScript e Bootstrap no layout compartilhado.

As coleções são usuarios, clientes, produtos, entregadores, pedidos e auditorias. Não estão implementados pagamento, estoque, emissão fiscal, geolocalização ou pedidos públicos. Faturamento significa valor dos pedidos válidos, não recebimento financeiro confirmado.

2. Requisitos funcionais

RF01. Autenticar por username e senha, manter sessão e permitir logout. Implementado com formulário e cookie JSESSIONID.

RF02. Trabalhar com ADMIN, GERENTE e ATENDENTE, verificando permissões no servidor. Implementado nas rotas e nos serviços protegidos.

RF03. Criar admin com senha inicial teste123 e perfil ADMIN quando essa conta não existir. Preservar conta já existente. Implementado com senha BCrypt.

RF04. Permitir ao ADMIN listar, consultar por ID, criar, editar e excluir usuários e atribuir perfis. Implementado. Criação exige senha não vazia e perfil.

RF05. Permitir edição da própria conta pela API sem elevação de perfil. Senha vazia mantém a atual; senha nova recebe BCrypt. Implementado. A tela geral de usuários continua exclusiva do ADMIN.

RF06. Listar funcionários com perfil GERENTE ou ATENDENTE, sem incluir ADMIN, para consulta de ADMIN e GERENTE. Implementado.

RF07. Cadastrar e editar clientes com nome, telefone, CEP, logradouro, número, complemento, bairro, cidade e estado. Manter endereco para dados antigos. Implementado para os três perfis; o backend não valida completamente obrigatoriedade e formato desses campos.

RF08. Consultar clientes pelos três perfis e permitir exclusão somente a ADMIN e GERENTE. Implementado. A consulta individual existe no formulário de edição, não como GET por ID na API.

RF09. Consultar ViaCEP no formulário para auxiliar o endereço. Implementado no JavaScript, dependente do serviço externo e sem teste automatizado próprio.

RF10. Manter produtos com nome, tipo, tipoPizza, sabor, preco e ativo. Aceitar Pizza, Lanche, Bebida e Opcional. Exigir nome, tipo e preço finito não negativo; Pizza exige tipoPizza e sabor. Implementado. Preço zero é permitido; ativo ausente assume false.

RF11. Permitir consulta de produtos pelos três perfis e manutenção somente por ADMIN e GERENTE. Implementado. Edição exige produto existente.

RF12. Manter entregadores separados das contas de acesso, com nome, telefone e disponivel. ADMIN e GERENTE mantêm o cadastro; os três perfis consultam a API para registrar a saída. Implementado. disponivel é armazenado, mas não impede a seleção na saída.

RF13. Permitir a GERENTE e ATENDENTE criar pedidos com cliente existente e ao menos um item com produto existente e quantidade positiva. Implementado. ADMIN consulta, mas não opera pedidos.

RF14. Obter preço e descrição do catálogo e calcular o total no servidor ao criar ou editar itens. Implementado. Preços enviados pelo cliente são substituídos; o preço do catálogo precisa ser finito e não negativo. A regra atual não rejeita o produto apenas por estar inativo.

RF15. Gerar ID, número, status RECEBIDO, responsável e data de entrada na criação, limpando dados de saída e cancelamento recebidos no corpo. Implementado. A numeração consulta o maior valor e soma um; concorrência não foi validada.

RF16. Permitir edição de pedido por GERENTE e ATENDENTE somente em RECEBIDO. Revalidar cliente e itens e recalcular o valor. Implementado. ATENDENTE não altera o desconto existente.

RF17. Listar pedidos para ADMIN, GERENTE e ATENDENTE. Implementado, sem paginação ou filtros na listagem geral.

RF18. Aceitar RECEBIDO para EM_PREPARACAO, EM_PREPARACAO para PRONTO, PRONTO para SAIU_PARA_ENTREGA ou RETIRADO e dessas duas situações para FINALIZADO. Impedir saltos, retrocessos e cancelamento por essa rota. Implementado.

RF19. Registrar a saída com entregador existente e nome preenchido, guardando ID, nome, telefone e horário. Implementado. Tanto entrega quanto retirada exigem entregador. responsavelSaida recebe o nome do entregador.

RF20. Permitir somente ao GERENTE cancelar em RECEBIDO, EM_PREPARACAO ou PRONTO, com justificativa não vazia. Implementado. A justificativa é aparada e auditada.

RF21. Permitir somente ao GERENTE aplicar ou alterar desconto entre zero e 20%, antes da saída ou retirada. Recalcular usando preços históricos dos itens persistidos. Implementado. ADMIN não herda essa permissão.

RF22. Preservar pedidos e rejeitar exclusão física. Implementado. DELETE existe, mas sempre é rejeitado pelo serviço.

RF23. Auditar inclusão, edição, status, desconto e cancelamento de pedidos, além de inclusão, edição e exclusão de clientes e usuários. Implementado. Produtos e entregadores não têm auditoria de manutenção.

RF24. Guardar usuário, perfil, horário, operação, entidade, ID, diferenças dos valores e justificativa. Omitir senha e hash do histórico de usuários. Implementado; entidade e auditoria são gravadas separadamente.

RF25. Permitir somente consulta de auditoria, por ADMIN e GERENTE. Implementado. Nenhum perfil altera ou exclui o histórico.

RF26. Mostrar dashboard em /dashboard para ADMIN e GERENTE, com faturamento, pedidos, pedidos válidos, ticket, em andamento e cancelados. Implementado.

RF27. Mostrar vendas por dia ou mês, distribuição por situação, dez produtos mais vendidos e vinte pedidos recentes. Implementado com gráfico SVG próprio e tabela de valores.

RF28. Filtrar o dashboard por início, fim, situação e agrupamento. Aceitar um a 366 dias inclusivos, preencher períodos vazios com zero e oferecer atalho para o mês atual. Implementado.

RF29. Atualizar o painel sem recarregar a página, manualmente ou a cada 60 segundos por opção do usuário. Mostrar carregamento, última consulta, período vazio e falhas. Implementado; atualização automática e perda de rede não têm teste automatizado de navegador.

RF30. Renderizar início, usuários, funcionários, clientes, produtos, entregadores, pedidos, auditoria, configurações e área administrativa conforme o perfil. Implementado. /configuracoes não tem API de alteração de configurações; /admin é um template separado de /dashboard.

3. Requisitos não funcionais

RNF01. Aplicar autorização no backend e negar rotas não autorizadas. Implementado com Spring Security. Os testes não cobrem cada método de todos os serviços.

RNF02. Codificar senhas com BCrypt e não retornar password no JSON. Implementado; password é somente de escrita na serialização de Usuario.

RNF03. Proteger escritas com CSRF e usar a sessão do login na API. Implementado. Não há JWT ou HTTP Basic configurado.

RNF04. Persistir em MongoDB e preservar preços e dados de saída históricos. Implementado no código, mas a integração real e a conversão de datas não foram verificadas pela suíte com mocks.

RNF05. Usar BigDecimal nos totais e indicadores, arredondamento monetário e proteção contra divisão por zero. Implementado. O catálogo guarda Double, convertido para BigDecimal nos itens.

RNF06. Manter o calendário gerencial e as datas persistidas consistentes com o fuso configurado no sistema. Não há migração automática de datas antigas.

RNF07. Apresentar textos em português, valores em reais e layout adaptável. Implementado no painel; existem capturas históricas de desktop e celular com dados fictícios. Não foi feita auditoria completa de acessibilidade.

RNF08. Oferecer rótulos, mensagens de estado e tabela alternativa ao gráfico. Implementado no dashboard; compatibilidade completa com leitores de tela ainda não foi certificada.

RNF09. Consultar período e campos necessários e limitar o painel a 366 dias, dez produtos e vinte pedidos recentes. Implementado. Agregação em memória, listagens gerais sem paginação e nenhuma meta de latência comprovada por teste de carga.

RNF10. Aplicar timeout de 20 segundos no cliente do dashboard, evitar consultas sobrepostas e pausar atualização com aba oculta. Implementado no JavaScript, sem teste automatizado de comportamento de rede.

RNF11. Diferenciar erro de ausência de vendas e responder 401/403 JSON na API e 503 tratado para falha de banco no dashboard. Implementado. As outras APIs não possuem tratamento uniforme específico de falhas do MongoDB.

RNF12. Separar controladores, regras, cálculo, auditoria e repositórios e permitir testes reproduzíveis pelo Maven. Implementado. O workflow verifica Java e sintaxe JavaScript em push/PR; não foi confirmada execução remota nesta revisão.

RNF13. Compilar e executar com JDK 21 e Maven Wrapper. Validado localmente com geração do JAR; dependências precisam estar disponíveis ou ser baixadas.

RNF14. Manter entidade e auditoria consistentes quando uma gravação falhar. Atendimento parcial: existem registros e testes dos dados, mas não há atomicidade comprovada entre as duas escritas. Exige evolução e teste de integração.

RNF15. Evitar números de pedido repetidos em criações simultâneas. Ainda não garantido: a implementação usa maior número mais um e não tem teste de concorrência.
