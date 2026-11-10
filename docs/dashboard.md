Este arquivo explica os indicadores, os filtros, a implementação e a forma de acessar o dashboard administrativo.

Dashboard administrativo do Pediu Patrão

O painel funcional em /dashboard usa dashboard.html, dashboard.css, dashboard.js, DashboardController e DashboardService. O gráfico é SVG próprio. Os arquivos AdminLTE existentes são separados desse painel operacional.

Faturamento soma valorTotal não cancelado, inclusive pedidos em andamento. Ticket divide faturamento por pedidos não cancelados. Em andamento exclui FINALIZADO e CANCELADO; entrega/retirada continuam em andamento até finalizar.

Ranking usa quantidade e preço histórico antes do desconto global. Seu valor bruto pode diferir do faturamento líquido. Produtos removidos preservam histórico. Empates de quantidade usam ID. Recentes são ordenados por entrada decrescente, com ID como desempate, limitados a vinte.

Filtros afetam todos os componentes. CANCELADO mantém contagem/lista, mas zera vendas, ticket e ranking. Este mês restaura mês atual, todas as situações e agrupamento diário. Atualização manual e automática opcional a cada 60 segundos, pausa com aba oculta e timeout de 20 segundos.

Consulta por dataHoraEntrada, início inclusivo à meia-noite e fim exclusivo à meia-noite seguinte ao dia final. Uma leitura com projeção alimenta o resumo, sem dados pessoais de clientes ou conexão. Agregação em memória. Sem data, o pedido fica fora. Valor total nulo vale zero. Itens incompletos não entram no ranking. Pedidos legados sem itens permanecem nos demais totais. Status nulo não é convertido em situação conhecida.

Índices sugeridos para avaliação no banco de teste, não aplicados por este documento:
use pediupatrao
db.pedidos.createIndex({dataHoraEntrada:1},{name:"dashboard_entrada"})
db.pedidos.createIndex({status:1,dataHoraEntrada:1},{name:"dashboard_status_entrada"})

Como executar e conferir

PowerShell deste computador:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd spring-boot:run

Em outras máquinas, ajuste JAVA_HOME para o JDK 21. Em Linux/macOS, use sh ./mvnw spring-boot:run.

Conexão: src/main/resources/application.yml. A configuração local atual contém URI e credenciais do Atlas fornecidas para teste, com banco pediupatrao. Este documento não reproduz as credenciais. .env não é carregado automaticamente. Sobreposição: SPRING_DATA_MONGODB_URI e SPRING_DATA_MONGODB_DATABASE. Confira application.properties local se houver conexão inesperada. O Atlas precisa permitir o IP e o acesso do usuário.

Entre em http://localhost:8080/login e abra http://localhost:8080/dashboard com ADMIN ou GERENTE. Banco vazio deve resultar em ausência de pedidos e totais zero. Operação real depende de conexão e inicialização bem-sucedidas, ainda não comprovadas pela suíte local.

Prévia sem banco no PowerShell:
.\mvnw.cmd test-compile '-Dexec.mainClass=com.umc.pediupatrao.preview.DashboardPreview' '-Dexec.classpathScope=test' '-Dexec.args=--server.port=8081 --server.address=127.0.0.1' org.codehaus.mojo:exec-maven-plugin:3.5.0:java

Abra http://127.0.0.1:8081, usuário demo e senha dashboard-demo. A prévia oferece só login/dashboard com dados fictícios, não registra pedidos reais e não é incluída no JAR. Encerre com Ctrl+C. O comando pode baixar o plugin Maven.

Testes: .\mvnw.cmd verify. Sintaxe: node --check src/main/resources/static/js/dashboard.js. Execução do pacote com Java 21: java -jar target/pediupatrao-1.0-SNAPSHOT.jar.

Os requisitos estão em docs/requisitos.md. Os endpoints e exemplos estão em docs/endpoints.md. A validação atual e seus limites estão em docs/testes.md.
