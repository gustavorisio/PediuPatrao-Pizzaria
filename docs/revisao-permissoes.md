# Validação da avaliação e correções

Revisão do código e execução de testes em 10/11/2026. As conclusões abaixo distinguem o estado encontrado das alterações desta revisão.

## Verificação das afirmações

| Afirmação | Validação no estado encontrado |
| --- | --- |
| Controle de perfis implementado | Correta. Existem ADMIN, GERENTE e ATENDENTE, autenticação por sessão, senhas com BCrypt e autorização por `@PreAuthorize`. |
| Gestão de pedidos e validação de status | Correta. `PedidoService` e `PedidoRegrasService` controlam criação, edição na situação inicial, sequência de status e saída. Valores são calculados no servidor com preços do catálogo na criação e preços históricos nos descontos posteriores. |
| Descontos e cancelamentos exclusivos do gerente | Correta. ADMIN não herda essa permissão. Cancelamento exige justificativa e ambos ficam restritos à etapa anterior à saída. Desconto limitado a 20%. |
| Auditoria persistente | Correta quanto à implementação: `AuditoriaService.registrar` grava em `AuditoriaRepository`, coleção `auditorias`, com usuário, perfil, data, operação e diferenças. Os testes verificam chamadas de persistência; não foi usado MongoDB real nesta revisão. |
| Cadastro de entregadores e separação em serviços | Correta. Entregadores possuem cadastro separado, e as regras se dividem em serviços de operação, validação, cálculo e auditoria. |
| Fragilidades nas permissões das rotas | Parcialmente correta. Os controladores já tinham autorização, mas a configuração HTTP terminava em `authenticated()`, vários serviços de cadastro não protegiam chamadas diretas e havia divergência entre a operação de saída e a consulta de entregadores. |
| Auditoria restrita apenas ao ADMIN | Correta no estado encontrado. Não é uma falha universal: trata-se de uma regra de acesso. Considerando a correção solicitada, a consulta foi ampliada a ADMIN e GERENTE, mantendo ATENDENTE bloqueado. |
| Ausência de testes automatizados | Incorreta para a versão atual: já havia 17 testes do dashboard. Faltava cobertura automatizada dos demais pontos citados, agora acrescentada. |
| Documentação de execução precisa de ajustes | Parcialmente correta. A orientação antiga de editar `target/` já tinha sido corrigida. Faltavam orientação de JDK/JAVA_HOME, variáveis para Linux/macOS e comando compatível com o wrapper sem permissão de execução. |

## Correções aplicadas

- Matriz de autorização HTTP explícita por recurso/método; rotas não reconhecidas são negadas. A edição da própria conta continua passando pela checagem de identidade no método.
- Respostas JSON 401 e 403 em toda a API; CSRF mantido nas operações de escrita. Páginas HTML mantêm login por formulário.
- Autorização também nos serviços de clientes, produtos, entregadores, consulta de pedidos e funcionários. Manutenção dos cadastros e exclusão de clientes continuam restritas à gestão.
- Consulta à auditoria liberada para ADMIN e GERENTE no serviço, API, página e menu. Não foram criadas operações de edição ou exclusão do histórico.
- ATENDENTE pode consultar entregadores via API para selecionar o responsável pela saída; não pode criar, editar, excluir nem acessar a tela de manutenção.
- Menu de gestão visível ao GERENTE, opção de usuários somente para ADMIN, estrutura HTML do submenu corrigida e remoção do link para `/horarios`, que não tinha controlador. Rota `/teste`, sem template correspondente, removida.
- Criação de pedidos limpa os campos de entregador/saída recebidos no payload, evitando dados de saída forjados em um pedido RECEBIDO.
- Verificação de desconto exige autenticação válida, e a identificação da própria conta rejeita autenticação anônima.
- README atualizado e cobertura de regressão ampliada.

## Matriz de permissões

| Operação | ADMIN | GERENTE | ATENDENTE |
| --- | --- | --- | --- |
| Consultar pedidos, clientes e produtos | Sim | Sim | Sim |
| Criar/editar pedidos e mudar status normal | Não | Sim | Sim |
| Cancelar ou aplicar/alterar desconto | Não | Sim | Não |
| Excluir pedido fisicamente | Não | Não | Não |
| Criar/editar clientes | Sim | Sim | Sim |
| Excluir clientes | Sim | Sim | Não |
| Manter produtos e entregadores | Sim | Sim | Não |
| Consultar entregadores via API para saída | Sim | Sim | Sim |
| Tela de cadastro de entregadores | Sim | Sim | Não |
| Consultar funcionários | Sim | Sim | Não |
| Administrar usuários e perfis | Sim | Não | Não |
| Editar a própria conta pela API, sem elevar perfil | Sim | Sim | Sim |
| Consultar auditoria e dashboard | Sim | Sim | Não |
| Alterar ou excluir auditoria | Não | Não | Não |

Mesmo GERENTE não exclui pedidos: a rota de exclusão é protegida e o serviço rejeita a operação para preservar o histórico. Editar a própria conta não dá acesso ao cadastro geral de usuários. Alterações de perfil são exclusivas do ADMIN.

## Testes e evidências

Comando: `mvn -B verify`, com JDK 21. Sem MongoDB, usando repositórios simulados, MockMvc com filtros reais de segurança e proxies reais de autorização dos serviços.

| Suíte | Casos | Cobertura |
| --- | --- | --- |
| DashboardControllerTest | 9 | API, autenticação, filtros e erros. |
| DashboardServiceTest | 8 | Indicadores, agrupamentos, limites e query. |
| PermissoesRotasTest | 40 | ADMIN/GERENTE/ATENDENTE, perfil desconhecido, acesso anônimo, páginas, APIs, CSRF, cancelamento/desconto e edição própria/alheia. |
| PedidoServiceTest | 16 | Cálculo confiável, cancelamento com justificativa antes da saída, limites de desconto, preservação de desconto pelo atendente, status, entregador, validação de dados, bloqueio de exclusão e gravação de auditoria. |
| SegurancaServicosTest | 11 | Autorização sem controlador, consulta à auditoria por ADMIN/GERENTE, bloqueio do atendente, proteção contra elevação de perfil e auditoria sem senha. |

**84 testes, zero falhas, zero erros e zero ignorados.** Os relatórios estão em [evidências da revisão](evidencias/revisao/) e o [resumo de execução](evidencias/revisao/verify.txt) preserva o resultado final. O workflow existente executa as suítes no GitHub quando houver push/PR; esta revisão não faz push.

## Limites

Não foi validada uma conexão com MongoDB real nem a atomicidade entre alteração de entidade e gravação de auditoria; as duas escritas existentes são separadas. Esta revisão comprova regras e tentativas de persistência nos testes, não uma transação de banco. A numeração sequencial concorrente de pedidos e testes de carga também estão fora desta verificação.

As capturas de interface em `docs/evidencias/dashboard-*.png` são da entrega anterior do dashboard com dados fictícios. As alterações de menu desta revisão foram verificadas por renderização dos templates nos testes HTTP; não foram geradas novas capturas.

## Texto revisado da avaliação

> O sistema implementa controle de perfis, gestão de pedidos, validação de transições, descontos e cancelamentos exclusivos do gerente, cadastro de entregadores e auditoria em MongoDB, com regras separadas em serviços. A revisão alinhou permissões das rotas, serviços e menus, permitiu consulta à auditoria por ADMIN e GERENTE e corrigiu o acesso do atendente à consulta de entregadores. Existem 84 testes automatizados aprovados, incluindo dashboard, segurança e regras operacionais. As instruções de execução foram atualizadas. A integração com MongoDB real e a atomicidade das gravações não foram verificadas nesta rodada.
