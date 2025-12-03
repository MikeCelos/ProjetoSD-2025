# Googol - Motor de Pesquisa Distribuído

**Sistemas Distribuídos 2025/2026** **Departamento de Engenharia Informática - Universidade de Coimbra**

---

## Autores (Grupo)
André Ramos nº2023227306

---

## Descrição do Projeto
Este projeto consiste num motor de busca web distribuído e tolerante a falhas ("Googol"), desenvolvido em duas fases:

1.  **Backend (Meta 1):** Sistema distribuído Java RMI com arquitetura de microsserviços:
    * **Barrels:** Armazenamento de dados replicado e persistente.
    * **Downloaders:** Web Crawlers paralelos que alimentam o índice.
    * **URL Queue:** Fila centralizada de URLs para distribuição de trabalho.
    * **Gateway:** Ponto de entrada e balanceador de carga para clientes.
    * **Multicast:** Protocolo fiável para replicação de dados entre Barrels.

2.  **Frontend (Meta 2):** Aplicação Web moderna desenvolvida em **Spring Boot (MVC)** que integra o sistema antigo e adiciona:
    * **WebSockets:** Monitorização do estado do cluster em tempo real (sem refresh).
    * **Integração REST:** Indexação de notícias via API do **Hacker News**.
    * **Inteligência Artificial:** Resumos automáticos de pesquisa gerados localmente via **Ollama**.

---

## Pré-requisitos
Para executar este projeto, o ambiente deve ter instalado:

* **Java JDK 21** (Obrigatório).
* **Maven** (Gestor de dependências e build).
* **Ollama** (Para a funcionalidade de IA).

### Configuração do Ollama (IA)
Este projeto utiliza o modelo `gemma3:270m` para gerar resumos rápidos e leves.

1.  Instale o Ollama: [https://ollama.com/](https://ollama.com/)
2.  No terminal, certifique-se que o serviço está a correr:
    ```bash
    ollama serve
    ```
3.  Numa nova janela, instale o modelo necessário:
    ```bash
    ollama pull gemma3:270m
    ```

---

## Como Compilar
Na raiz do projeto (onde está o `pom.xml`), execute:

```bash
mvn clean compile

 Guia de Execução (Passo a Passo)

Como se trata de um sistema distribuído, é necessário iniciar os componentes em terminais separados, respeitando estritamente a seguinte ordem:
1. Camada de Armazenamento (Barrels)

Inicie pelo menos dois barrels para garantir sincronização.

    Terminal 1 (Barrel 0):
    Bash

java -cp target/classes pt.uc.sd.googol.barrel.BarrelLauncher 0

Terminal 2 (Barrel 1):
Bash

    java -cp target/classes pt.uc.sd.googol.barrel.BarrelLauncher 1

2. Gestão de Tarefas (Queue)

    Terminal 3 (URL Queue):
    Bash

    java -cp target/classes pt.uc.sd.googol.downloader.QueueServer

3. Ponto de Acesso (Gateway)

    Terminal 4 (Gateway RMI):
    Bash

    java -cp target/classes pt.uc.sd.googol.gateway.Gateway

4. Web Crawlers (Downloaders)

Pode iniciar múltiplos para acelerar a indexação.

    Terminal 5 (Downloader):
    Bash

    mvn exec:java -Dexec.mainClass="pt.uc.sd.googol.downloader.Downloader"

5. Aplicação Web (Frontend)

Este comando inicia o servidor web Spring Boot na porta 8080.

    Terminal 6 (Web Server):
    Bash

    mvn spring-boot:run

Como Usar

Após iniciar todos os terminais, abra o browser em:

http://localhost:8080
Funcionalidades Disponíveis:

    Pesquisa: Digite termos (ex: "universidade") para ver resultados do índice distribuído.

        Nota: A análise de IA será gerada automaticamente no topo dos resultados.

    Hacker News: Pesquise um termo preferencialmente em inglês (ex: "AI", "Linux") e clique em "Indexar top stories" para povoar o índice com notícias reais.

    Adicionar URL: Insira um link manualmente para furar a fila e ser indexado com prioridade.

    Dashboard: A barra lateral direita atualiza-se sozinha (WebSockets) mostrando os recursos ativos.

Notas Técnicas

    Persistência: Se um Barrel for reiniciado, ele recupera os dados do ficheiro .dat local ou sincroniza-se automaticamente com outro Barrel ativo.

    Tolerância a Falhas: O Gateway e o Downloader reconectam-se automaticamente se um Barrel falhar.

    IA Offline: O sistema usa Ollama local para garantir privacidade e funcionamento sem custos de API.