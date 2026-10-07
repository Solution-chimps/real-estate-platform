# 🏠 Real Estate Platform — Backend

Backend da plataforma imobiliária responsável pelo gerenciamento de imóveis, usuários, clientes e operações administrativas da aplicação.

O projeto foi desenvolvido utilizando **Java + Spring Boot**, seguindo uma arquitetura organizada em camadas e preparada para integração com o frontend em Angular.

---

## 🚀 Sobre o projeto

A **Real Estate Platform** tem como objetivo centralizar a operação de uma imobiliária em uma única plataforma.

O sistema possui dois principais contextos:

- 🌐 **Portal público** — destinado aos clientes que desejam consultar imóveis.
- 🔐 **Backoffice** — destinado aos usuários internos da imobiliária para gerenciamento da plataforma.

Este repositório contém exclusivamente o **backend da aplicação**, responsável pela API REST, regras de negócio, persistência dos dados e autenticação/autorização.

---

## 🛠️ Tecnologias

- ☕ Java
- 🌱 Spring Boot
- 🌐 Spring Web
- 🔐 Spring Security
- 🗄️ Spring Data JPA
- 🐬 MySQL
- 📦 Maven
- 📖 OpenAPI / Swagger
- 🐳 Docker

---

## 🏗️ Arquitetura

O projeto utiliza uma arquitetura baseada na separação de responsabilidades entre as principais camadas da aplicação:

```text
src
└── main
    └── java
        └── ...
            ├── controller
            ├── service
            ├── repository
            ├── entity
            ├── dto
            ├── mapper
            ├── exception
            ├── config
            └── security