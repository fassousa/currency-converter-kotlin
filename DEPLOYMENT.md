# Deployment Guide — Kotlin/Spring Boot

> Mesma estratégia do projeto Ruby, adaptada para Docker + Spring Boot no mesmo Droplet DigitalOcean.

## Ambiente de Produção

- **URL:** http://143.198.21.206 (HTTPS após configurar DuckDNS + Certbot)
- **Swagger UI:** http://143.198.21.206:8080/api/v1/swagger-ui/index.html
- **Platform:** Digital Ocean Droplet `currency-converter-kotlin` — 2 GB / 50 GB / NYC3 / Ubuntu 22.04
- **Stack:** Nginx + Docker Compose (Spring Boot + PostgreSQL + Redis)
- **SSL:** Let's Encrypt (a configurar — ver seção abaixo)

---

## Deploy Automático (CI/CD)

Push para `main` dispara automaticamente:

1. **Test & Lint** — `./gradlew test` + `./gradlew detekt`
2. **Build** — `docker build` cria a imagem multi-stage
3. **Deploy** — copia imagem + `docker-compose.prod.yml` via SCP, sobe containers
4. **Health Check** — verifica `GET /api/v1/health`
5. **Rollback automático** se o health check falhar

---

## Configuração Inicial do Servidor (única vez)

### 1. Acesse o novo Droplet

```bash
ssh root@143.198.21.206
```

### 2. Instale Docker e Docker Compose

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
```

### 3. Crie a pasta do projeto

```bash
mkdir -p ~/currency-converter-kotlin/nginx
```

### 4. Configure um domínio DuckDNS (grátis)

1. Acesse https://www.duckdns.org e faça login
2. Crie um subdomínio, ex: `kotlin-converter.duckdns.org`
3. Aponte para o IP `143.198.21.206`
4. No servidor, instale o Certbot e gere o certificado:

```bash
# No Droplet:
apt install -y certbot
certbot certonly --standalone -d kotlin-converter.duckdns.org
```

5. Atualize o `nginx/nginx.conf` com o novo domínio:
```bash
sed -i 's/currencyconverter.duckdns.org/kotlin-converter.duckdns.org/g' ~/currency-converter-kotlin/nginx/nginx.conf
```

### 6. Configure os Secrets no GitHub

No repositório do Kotlin, vá em **Settings → Secrets and variables → Actions** e adicione:

| Secret | Valor |
|---|---|
| `DEPLOY_HOST` | `161.35.142.103` |
| `DEPLOY_USER` | `deploy` |
| `DEPLOY_SSH_KEY` | Chave SSH privada (a mesma usada no Ruby) |
| `DB_USER` | usuário do PostgreSQL |
| `DB_PASSWORD` | senha do PostgreSQL |
| `JWT_SECRET` | segredo JWT (mín. 32 chars) |
| `CURRENCY_API_KEY` | chave da API de câmbio |

---

## Deploy Manual

```bash
ssh root@143.198.21.206
cd ~/currency-converter-kotlin

# Build local e copia
docker build -t currency-converter-kotlin:latest .
docker save currency-converter-kotlin:latest | gzip > image.tar.gz
# (ou scp a imagem do local)

# Sobe
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --no-build
```

---

## Health Check

```bash
curl http://143.198.21.206:8080/api/v1/health
```

Resposta esperada:
```json
{
  "status": "healthy",
  "version": "1.0.0",
  "checks": {
    "database": { "status": "up" },
    "cache":    { "status": "up" },
    "external_api": { "status": "up", "message": "API key is configured" }
  }
}
```

---

## Comandos úteis no servidor

```bash
# Ver logs da aplicação
docker logs -f currency-converter-app

# Reiniciar apenas o app (sem derrubar DB/Redis)
docker compose -f docker-compose.prod.yml restart app

# Ver status dos containers
docker compose -f docker-compose.prod.yml ps

# Rollback manual para imagem anterior
docker compose -f docker-compose.prod.yml down
docker tag currency-converter-kotlin:rollback currency-converter-kotlin:latest
docker compose -f docker-compose.prod.yml --env-file .env.production up -d --no-build
```

---

## Diferenças em relação ao Deploy do Ruby

| | Ruby | Kotlin |
|---|---|---|
| App Server | Puma (processo nativo) | Spring Boot (container Docker) |
| Proxy | Nginx nativo (`systemctl`) | Nginx container (Docker Compose) |
| Deploy | `git pull` + `bundle install` + `systemctl restart puma` | `docker load` + `docker compose up` |
| Rollback | `git checkout <sha>` | `docker tag rollback latest` + `compose up` |
| DB Migrations | `rails db:migrate` | Flyway automático no boot |
| Dependências | `Gemfile.lock` | imagem Docker já contém tudo |

---

**CI/CD completo:** `.github/workflows/ci-cd.yml`






