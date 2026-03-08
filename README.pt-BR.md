# Currency Converter API 💱

> 🌍 **Idioma:** [English](README.md) | **Português**

> **Desenvolvedor Kotlin/Spring Boot Sênior - Avaliação Técnica para Jaya Tech**

API Spring Boot pronta para produção para conversão de moedas em tempo real com autenticação JWT, cobertura completa de testes e deployment automatizado via CI/CD.

🌐 **Live:** https://kotlin-converter.duckdns.org | 📚 **Docs API:** https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html | ✅ **80% de cobertura** (JaCoCo)

---

## ✅ Requisitos da Avaliação Atendidos

| Requisito | Implementação | Evidência |
|-----------|---------------|-----------|
| **Spring Boot 3.2+** | ✅ Spring Boot 3.2.3 | [build.gradle.kts](build.gradle.kts) |
| **Kotlin 1.9+** | ✅ Kotlin 1.9.22 | [build.gradle.kts](build.gradle.kts) |
| **PostgreSQL** | ✅ BD em Produção | Migrações Flyway em `src/main/resources/db/migration` |
| **Redis** | ✅ Cache & Rate Limiting | [application.yml](src/main/resources/application.yml) |
| **Testes JUnit** | ✅ Cobertura abrangente, 80% de cobertura de linhas | `./gradlew test` |
| **CI/CD** | ✅ GitHub Actions | `.github/workflows/ci-cd.yml` |
| **Git & Ágil** | ✅ PRs, commits convencionais | [Histórico de commits](https://github.com/your-org/currency-converter-kotlin/commits/main) |

**Bônus:** Docker ✅ | Análise Estática (Detekt) ✅ | Segurança (Spring Security + JWT) ✅ | Documentação API (Springdoc OpenAPI) ✅ | Deploy em Produção ✅ | HTTPS/SSL ✅ | Resilience4j (Circuit Breaker) ✅ | Rate Limiting (Bucket4j) ✅

---

## 🚀 Início Rápido (30 segundos)

```bash
cd currency-converter-kotlin
./gradlew bootRun
```

Ou com Docker:

```bash
docker-compose up
```

**Login de teste:** `admin@example.com` / `password`  
**Experimente:** Visite http://localhost:8080/api/v1/swagger-ui/index.html para documentação interativa da API

---

## 🏗️ Arquitetura & Decisões Técnicas

**Padrões de Design:**
- Service Objects para isolamento de lógica de negócio
- OpenFeign Client para chamadas de API externa com decoradores Resilience4j
- Hierarquia de exceções customizada com mapeamento apropriado de códigos HTTP
- Padrão Repository com Spring Data JPA

**Performance:**
- Cache Redis (TTL de 24h para taxas de câmbio)
- Índices de banco de dados em chaves estrangeiras e colunas de busca
- Prevenção de queries N+1 com eager loading e projections
- Connection pooling com HikariCP

**Segurança:**
- Autenticação JWT (Spring Security + JJWT)
- Rate limiting: 100 req/min por usuário (Bucket4j + Redis)
- Análise estática: Detekt com regras customizadas
- HTTPS com certificado SSL Let's Encrypt
- Resilience4j Circuit Breaker para chamadas de API externa

**Garantia de Qualidade:**
- JUnit 5 com MockK (biblioteca de mock idiomática para Kotlin)
- Testcontainers para testes de integração (PostgreSQL, Redis)
- WireMock para mocking de API externa
- 80% de cobertura de código (JaCoCo)
- Pipeline CI/CD com testes, linting e deployment automatizados

📖 **Aprofunde-se:** [Decisões de Arquitetura](KOTLIN_IMPLEMENTATION_PLAN.md) | [Guia de Desenvolvimento](DEVELOPMENT.md) | [Guia de Deployment](DEPLOYMENT.md)

---

## 📋 Funcionalidades Principais

- ✅ **10+ moedas** com taxas de câmbio em tempo real ([CurrencyAPI](https://currencyapi.com))
- ✅ **Autenticação JWT** para acesso seguro à API (Spring Security)
- ✅ **Histórico de transações** com paginação e isolamento por usuário
- ✅ **Logging abrangente** com JSON estruturado (Logstash Logback)
- ✅ **Health checks** para monitoramento (banco de dados, cache, API externa)
- ✅ **Documentação Swagger/OpenAPI** auto-gerada a partir de anotações
- ✅ **Resilience4j Circuit Breaker** para tolerância a falhas
- ✅ **Rate limiting** com Bucket4j (100 req/min por usuário)
- ✅ **Métricas Prometheus** para monitoramento (Micrometer)

---

## 🧪 Testes & Qualidade

```bash
./gradlew test              # Executar todos os testes (JUnit 5)
./gradlew detekt            # Lint de estilo de código com Detekt
./gradlew jacocoTestReport  # Gerar relatório de cobertura
open build/reports/jacoco/test/html/index.html  # Ver cobertura
```

**Detalhamento da Cobertura de Testes:**
- Controllers: Testes de integração com Testcontainers (PostgreSQL, Redis)
- Services: Testes unitários com MockK
- OpenFeign Clients: Servidor stub WireMock
- Repositories: Testes de banco de dados com Testcontainers
- Tratamento de exceções: Specs de mapeamento de exceções customizadas

**Stack de Framework de Testes:**
- JUnit 5 (Jupiter)
- MockK + SpringMockK
- Testcontainers (PostgreSQL, Redis)
- WireMock (stubs HTTP)
- AssertJ (asserções fluentes)

---

## 📚 Documentação

- 📖 [Exemplos de API](src/main/resources/docs/API_EXAMPLES.md) - Exemplos de requisição/resposta
- 📖 [Decisões de Arquitetura](KOTLIN_IMPLEMENTATION_PLAN.md) - Escolhas técnicas & justificativas
- 📖 [Guia de Desenvolvimento](DEVELOPMENT.md) - Setup local & workflows Docker
- 📖 [Guia de Deployment](DEPLOYMENT.md) - Setup de produção com HTTPS
- 📖 [Docs API Interativas](https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html) - Swagger UI
- 📖 [Schema OpenAPI](https://kotlin-converter.duckdns.org/api/v1/api-docs) - Especificação JSON

---

## 🛠️ Stack Tecnológica

**Linguagem & Framework:** Kotlin 1.9 | Spring Boot 3.2 | Spring Security  
**Banco de Dados:** PostgreSQL | Flyway (migrações) | Spring Data JPA  
**Cache & Rate Limiting:** Redis | Bucket4j | Lettuce  
**HTTP Client:** OpenFeign | Resilience4j (Circuit Breaker, Retry)  
**Testes:** JUnit 5 | MockK | Testcontainers | WireMock | AssertJ  
**Qualidade:** Detekt | JaCoCo (80% cobertura) | Micrometer (Prometheus)  
**DevOps:** GitHub Actions | Docker | Docker Compose | Nginx + Spring Boot  
**Documentação API:** Springdoc OpenAPI | Swagger UI  
**Logging:** SLF4J | Logstash Logback Encoder (logs JSON estruturados)  

---

## 🚀 Workflow de Desenvolvimento

### Pré-requisitos
- Java 21+
- Kotlin 1.9+
- PostgreSQL 14+ (ou use Docker Compose)
- Redis 7+ (ou use Docker Compose)

### Desenvolvimento Local

**1. Inicie a infraestrutura (PostgreSQL + Redis):**
```bash
docker-compose up -d postgres redis
```

**2. Execute a aplicação:**
```bash
./gradlew bootRun
```

**3. Execute os testes:**
```bash
./gradlew test
```

**4. Execute análise estática:**
```bash
./gradlew detekt
```

**5. Gere relatório de cobertura:**
```bash
./gradlew jacocoTestReport
```

---

## 🌟 Por Que Esta Implementação?

**Para a "Engenharia de Software Consciente" da Jaya Tech:**

1. **Decisões Baseadas em Dados:** Cobertura de 80% com JaCoCo garante métricas de qualidade
2. **Relacionamentos Saudáveis:** Arquitetura limpa facilita colaboração em equipe e manutenibilidade
3. **Compreensão do Impacto:** Documentação explica o *porquê*, não apenas o *o quê*
4. **Autoconhecimento:** Cada commit segue convenções, testes validam premissas

**Funcionalidades Prontas para Produção:**
- Deploy com CI/CD, não apenas "funciona na minha máquina"
- Análise estática (Detekt) detecta problemas de qualidade de código cedo
- Suite de testes abrangente com Testcontainers para cenários realistas
- Certificado SSL real, não placeholders auto-assinados
- Logging JSON estruturado para debugging e monitoramento
- Resilience4j para lidar com falhas de API externa graciosamente
- Métricas Prometheus para observabilidade e alerting

---

## 🔧 Configuração

Toda configuração é externalizada via `application.yml`:

```yaml
spring:
  application:
    name: currency-converter-api
  datasource:
    url: jdbc:postgresql://postgres:5432/currency_converter
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: validate
  redis:
    host: redis
    port: 6379
  cache:
    type: redis
    redis:
      time-to-live: 86400000  # 24 horas em ms

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

app:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 86400000  # 24 horas em ms
  currency-api:
    key: ${CURRENCY_API_KEY}
    base-url: https://api.currencyapi.com/v3
```

---

## 📊 Monitoramento & Observabilidade

**Health Checks:** `GET /api/v1/health`
- Conectividade de banco de dados
- Conectividade Redis
- Configuração de API externa

**Métricas Prometheus:** `GET /actuator/prometheus`
- Latência e contagem de requisições HTTP
- Métricas de memória e GC da JVM
- Métricas do pool de conexão de banco de dados
- Métricas de negócio customizadas

**Logging Estruturado:**
- Todos os logs em formato JSON (Logstash Logback)
- Inclui ID de requisição para rastreamento
- Propagação automática de contexto

---

## 🐳 Deploy com Docker

**Construa a imagem:**
```bash
docker build -t currency-converter-kotlin:latest .
```

**Execute com Docker Compose:**
```bash
docker-compose -f docker-compose.prod.yml up
```

O Dockerfile usa construção em múltiplos estágios:
1. **Estágio Builder:** Compila Kotlin, executa testes, gera cobertura
2. **Estágio Runtime:** Imagem JRE 21 mínima com JAR da aplicação

---

**Desenvolvido com ❤️ usando Kotlin + Spring Boot** | [Ver Aplicação Live →](https://kotlin-converter.duckdns.org/api/v1/swagger-ui/index.html)

