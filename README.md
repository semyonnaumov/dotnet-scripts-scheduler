# dotnet-scripts-scheduler

Part of [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts) project. For complete reference
address [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts).

Service, responsible for communication with clients via RESTful API and scheduling tasks for
[workers](https://github.com/semyonnaumov/dotnet-scripts-worker).

⚠️ Full description is currently available only in Russian.

## 1. Описание

Модуль, ответственный за все взаимодействие с клиентами, передачу задач воркерам, хранение, обновление и удаление данных
о задачах в своей БД.

Стандартный флоу работы с задачей: получение задачи от клиента, создание записи о задаче в БД, отправка задачи в
соответствующий типу worker-а топик Kafka (`pending-${worker_type}`). Параллельно происходит сбор информации из
топиков `running` и `finished` с последующим обновлением данных о запусках в БД, а так же удаление полученных клиентом
или невостребованных результатов запусков в соответствии с определенными политиками.

## 2. API

### 2.1 Служебный API

| Эндпойнт             | Описание                      |
|----------------------|-------------------------------|
| GET /actuator/health | Проверка состояния приложения |
| GET /swagger-ui      | Swagger UI с описанием API    |
| GET /api-docs        | Swagger JSON                  |
| GET /api-docs.yaml   | Swagger YAML                  |

### 2.2 Прикладной API

| Эндпойнт               | Описание                               |
|------------------------|----------------------------------------|
| POST /jobs             | Отправка новой задачи на выполнение    |
| GET /jobs/{id}         | Получение полной информации о задаче   |
| GET /jobs/{id}/request | Получение запроса задачи               |
| GET /jobs/{id}/status  | Получение текущего статуса задачи      |
| GET /jobs/{id}/result  | Получение результата выполнения задачи |
| DELETE /jobs/{id}      | Удаление задачи                        |

Детальное описание прикладного API можно посмотреть в Swagger UI:
[http://localhost:8080/swagger-ui](http://localhost:8080/swagger-ui)

## 3. Запуск dotnet-scripts-scheduler

Для запуска необходимо сначала поднять обвязку с кафкой и базой данных при помощи docker compose:

```bash
docker compose up -d
```

После этого можно собрать и запустить приложение:

```bash
./gradlew bootRun
```

Также можно запускать приложение в Docker-контейнере. Для этого нужно собрать образ приложения:

```bash
docker build -t dotnet-scripts-scheduler:latest .
```

И запустить из него контейнер, подключив к сети из вышеуказанного docker-compose.yaml:

```bash
docker run --name dotnet-scripts-scheduler \
    --network=dotnet-scripts-scheduler_dss-network -it --rm \
    -p 8080:8080 \
    -e SCHEDULER_POSTGRES_URL=postgres:5432 \
    -e SCHEDULER_KAFKA_BROKER_URL=kafka-broker-1:9092 \
    dotnet-scripts-scheduler:latest
```

⚠️ Запуск планировщика совместно с воркером описан в [dotnet-scripts](https://github.com/semyonnaumov/dotnet-scripts). 

## 4. Взаимодействие с приложением

Примеры запросов к приложению:

1. Отправка скрипта на выполнение:

    ```bash
    curl --request POST 'localhost:8080/jobs' \
    --header 'Content-Type: application/json' \
    --data-raw '{
        "requestId": "request-1",
        "senderId": "sender-1",
        "payload": {
            "script": "Console.WriteLine(\"Hello from from job from sender-1\");",
            "jobConfig": {
                "nugetConfigXml": "<?xml version=\"1.0\" encoding=\"utf-8\"?><configuration><packageSources><add key=\"NuGet official package source\" value=\"https://nuget.org/api/v2/\" /></packageSources><activePackageSource><add key=\"All\" value=\"(Aggregate source)\" /></activePackageSource></configuration>"
            },
            "agentType": "linux-amd64-dotnet-7"
        }
    }'
    ```

   Сервис вернет идентификатор созданной джобы:

    ```json
    {
        "jobId": "c0a86402-8646-18b3-8186-4688df130000"
    }
    ```

   ⚠️ Внимание! Код ответа `200` означает, что отправленная джоба уже была создана ранее, и еще раз запускаться не
   будет, потому что найден имеющийся запуск с таким же `requestId`. Для того чтобы создалась новая джоба, нужен
   новый `requestId`. Код ответа `201` свидетельствует о создании новой джобы.


2. Получение полной информации о созданной джобе (нужно указать правильный идентификатор):

    ```bash
    curl --request GET 'localhost:8080/jobs/c0a86402-8646-18b3-8186-4688df130000'
    ```

3. Удаление джобы:

    ```bash
    curl --request DELETE 'localhost:8080/jobs/c0a86402-8646-18b3-8186-4688df130000'
    ```

Для того чтобы посмотреть сообщения с задачами для воркера, отправляемые планировщиком в кафку, нужно подключиться к
контейнеру с брокером кафки и запустить в нем консьюмер топика `pending-linux-amd64-dotnet-7` с
консьюмер-группой `console`:

```bash
docker exec -it dss-kafka-broker-1 sh
```

```bash
kafka-console-consumer --bootstrap-server localhost:9092 \
    --topic pending-linux-amd64-dotnet-7 --from-beginning --group console
```

Чтобы отправлять сообщения приложению можно подключиться к топикам `running` или `finished`:

```bash
kafka-console-producer --bootstrap-server localhost:9092 \
    --topic running --property "parse.key=true" --property "key.separator=:"
```

```bash
kafka-console-producer --bootstrap-server localhost:9092 \
    --topic finished --property "parse.key=true" --property "key.separator=:"
```

В качестве ключа для отправки можно использовать что угодно, ключ не читается приложением, но используется кафкой для
определения партиции, в которую отправится сообщение (воркер использует
идентификатор джобы в качестве ключа, когда отправляет сообщения). Структура сообщений определяется DTO-классами
пакета `com.naumov.dotnetscriptsscheduler.dto.kafka`. Примеры сообщений:

В `running`:

```bash
key1:{"jobId": "7f000001-863c-11c6-8186-3cc292d00000"}
```

В `finished`:

```bash
key1:{"jobId": "7f000001-863c-11c6-8186-3cc292d00000","status": "REJECTED"}
```

```bash
key1:{"jobId": "7f000001-863c-11c6-8186-3cc292d00000","status": "ACCEPTED", "scriptResults": {"finishedWith": "TIME_LIMIT_EXCEEDED", "stdout": "some stdout", "stderr": "some stderr"}}
```

