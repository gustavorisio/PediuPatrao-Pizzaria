# Pediu Patrão - Pizzaria

Projeto de um sistema para ajudar no controle de uma pizzaria. Pelo sistema, é possível cadastrar produtos, clientes, usuários(gerente, atendente, admin), entregadores e acompanhar os pedidos.

## Requisitos

Para executar o projeto, é necessário ter instalado:

- Java 21;
- MongoDB, podendo ser local ou MongoDB Atlas;
- Maven (o projeto também possui o arquivo `mvnw`, que pode ser usado sem instalar o Maven).

## Como executar

Editor e inicializador recomendado: Visual Studio Code.
1. Clone o projeto e abra a pasta no terminal.
2. Configure o acesso ao MongoDB usando as variáveis abaixo, com o nome do arquivo se chamando: application.properties.
3. o arquivo application.properties deve ser alocado na pasta src/target/classes/, contendo:

   - `MONGODB_URI`: endereço de conexão do banco;
   - `MONGODB_USERNAME`: usuário do banco, quando necessário;
   - `MONGODB_PASSWORD`: senha do banco, quando necessário.
  
```
MONGODB_USERNAME=usuario_mongodb
MONGODB_PASSWORD=senha_mongodb
MONGODB_URI=exemplo: mongodb+srv://...
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=pediupatrao
```

3. Execute o projeto.

   No Windows:

   ```bash
   .\mvnw.cmd spring-boot:run
   ```

   No Linux ou macOS:

   ```bash
   ./mvnw spring-boot:run
   ```

4. Acesse no navegador:

   ```text
   http://localhost:8080
   ```

Na primeira execução, o sistema cria um usuário administrador para o acesso inicial:

- Usuário: `admin`
- Senha: `teste123`

Essa senha deve ser alterada depois do primeiro acesso, em editar usuario.

## Requisitos implementados

### RF01 - Controle de usuários e permissões

- Foram implementados os três perfis pedidos: `ADMIN`, `GERENTE` e `ATENDENTE`.
- As permissões são verificadas no backend, inclusive nas APIs.
- O administrador controla os usuários e seus perfis.
- O usuário comum pode alterar os próprios dados, mas não pode mudar o próprio perfil para ganhar mais acesso.
- As senhas são protegidas antes de serem salvas.

### RF02 - Evolução do cadastro de pedidos

- O pedido possui cliente, itens, quantidades, produtos, desconto, valor total e situação.
- O sistema registra quem lançou o pedido e a data e hora de entrada.
- O valor dos itens e o total são calculados no servidor.
- O pedido só pode ser criado com cliente cadastrado, produtos existentes e quantidades maiores que zero.
- O cadastro e a edição do pedido são permitidos somente enquanto ele ainda está na situação inicial.

### RF03 - Entrada, saída e transições

- Os pedidos possuem situações definidas: recebido, em preparação, pronto, saiu para entrega, retirado, finalizado e cancelado.
- O backend bloqueia mudanças fora da ordem permitida.
- Ao registrar a saída, é obrigatório informar um entregador cadastrado quando necessário.
- São salvos o responsável, a data e a hora do lançamento, além dos dados da saída.

### RF04 - Cancelamentos e descontos

- Somente o gerente pode cancelar um pedido.
- O cancelamento exige uma justificativa e só pode ocorrer antes da saída do pedido.
- Somente o gerente pode aplicar ou alterar descontos.
- O desconto é validado e não pode passar de 20%.
- Pedidos excluídos não são apagados: o sistema impede a exclusão e mantém o histórico.

### RF05 - Auditoria

- As alterações de pedidos são salvas em uma coleção de auditoria.
- As inclusões, alterações e exclusões de clientes também são registradas.
- Cada registro guarda o usuário, o perfil, a data e a hora, a operação, o item alterado e os valores anteriores e novos.
- Cancelamentos guardam também a justificativa.
- A consulta da auditoria é restrita ao administrador.
- Os registros de auditoria podem ser consultados, mas não possuem funções para alteração ou exclusão.

### Qualidade do sistema

- O projeto está separado em controladores, serviços, entidades e repositórios.
- As regras principais ficam nos serviços do backend.
- Os erros de validação e de regra de negócio retornam mensagens para informar o motivo da operação não ser aceita.
- As informações ficam armazenadas no MongoDB.

## Principais alterações realizadas no backend

- Foram criadas e organizadas rotas para produtos, clientes, pedidos, usuários, entregadores e auditorias.
- Foram adicionadas as regras dos três perfis e a proteção das operações de acordo com cada permissão.
- O cadastro de pedidos foi ampliado com itens, quantidades, responsáveis, entrada, saída, entregador e valor total.
- Foram adicionadas validações para clientes, produtos, quantidades, preços, descontos e situações dos pedidos.
- Foram implementados os bloqueios para cancelamento e desconto exclusivos do gerente.
- Foi criado o registro de auditoria para pedidos, clientes e usuários, com os valores antes e depois das alterações.

## Tecnologias usadas

- Java 21;
- Spring Boot;
- Spring Web;
- Spring Security;
- Spring Data MongoDB;
- Thymeleaf;
- Maven;
- MongoDB.

## Contribuidores
- Professor: Alessandro Aparecida da Silva Horas
- Implementado por: Gustavo Di Risio
