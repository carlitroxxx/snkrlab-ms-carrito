# snkrlab-ms-carrito

Microservicio de carrito de compras del sistema **SNKRLAB**. Gestiona los ítems del carrito por usuario y el checkout, consumiendo `snkrlab-ms-productos` mediante Feign para validar productos y descontar stock.

## Herramientas

- Java 21 · Spring Boot 4.1.1
- Spring Data JPA + MySQL Connector/J
- Spring Cloud OpenFeign (cliente hacia `ms-productos`)
- Spring Security + OAuth2 Resource Server (JWT de Azure AD)
- Lombok
- Docker

## Modelo de datos

**ItemCarrito** (tabla `items_carrito`)

| Campo | Tipo | Notas |
|---|---|---|
| `id` | Long | autogenerado |
| `usuarioId` | String | tomado del claim `oid` del JWT, no lo envía el cliente |
| `productoId` | Long | requerido |
| `cantidad` | Integer | requerido |
| `precioUnitario` | Integer | requerido |

## Variables de entorno

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `DB_URL` | URL JDBC de MySQL | `jdbc:mysql://localhost:3306/snkrlab_cart_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | Usuario de MySQL | `root` |
| `DB_PASSWORD` | Password de MySQL | `root` |
| `APPLICATION_CONFIG_PRODUCTOS_URL` | Host base para llegar a `ms-productos` | `http://localhost:8082` |
| `AZURE_ISSUER_URI` | Base del issuer de Azure AD | `https://login.microsoftonline.com/` |
| `AZURE_TENANT_ID` | Tenant ID de Azure AD | `db9d1cc0-8c32-4341-bc72-c348daf096fb` |

> El `ProductoClient` (Feign) llama internamente a `/desarrollo/api/v1/productos/{id}` y `/desarrollo/api/v1/productos/{id}/stock`. Ese prefijo `/desarrollo` corresponde al **stage del API Gateway**: en despliegue, `APPLICATION_CONFIG_PRODUCTOS_URL` debe apuntar al dominio del API Gateway **sin** el stage (ej. `https://<api-id>.execute-api.us-east-1.amazonaws.com`), y el stage debe llamarse `desarrollo` para que la ruta final calce.

El header `Authorization` de la petición entrante se reenvía automáticamente a `ms-productos` (`FeignClientConfig`).

## Ejecutar en local

Requiere `ms-productos` corriendo en `http://localhost:8082` y MySQL con la base `snkrlab_cart_db`.

```bash
./mvnw spring-boot:run
```

Corre en `http://localhost:8083`.

## Ejecutar con Docker

```bash
docker run -d --name mysql-carrito \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=snkrlab_cart_db \
  -p 3306:3306 mysql:8

docker build -t ms-carrito:latest .
docker run -d --name ms-carrito \
  -p 8083:8083 \
  -e DB_URL="jdbc:mysql://<host-mysql>:3306/snkrlab_cart_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true" \
  -e APPLICATION_CONFIG_PRODUCTOS_URL="https://<api-id>.execute-api.us-east-1.amazonaws.com" \
  -e AZURE_TENANT_ID="<tenant-id>" \
  ms-carrito:latest
```

## Endpoints

Base path: `/api/v1/carrito` — **todos requieren** `Authorization: Bearer <token>` (el `usuarioId` se toma del claim `oid` del JWT).

| Método | Path | Descripción |
|---|---|---|
| GET | `/` | Lista los ítems del carrito del usuario autenticado |
| POST | `/` | Agrega un ítem al carrito |
| PUT | `/item/{id}` | Actualiza un ítem del carrito |
| DELETE | `/item/{id}` | Elimina un ítem del carrito |
| DELETE | `/` | Vacía el carrito completo |
| POST | `/checkout` | Confirma la compra: devuelve `mensaje`, `cantidadItems` y `total` |

### Ejemplo `POST /api/v1/carrito`

```json
{
  "productoId": 3,
  "cantidad": 2,
  "precioUnitario": 89990
}
```

### Ejemplo respuesta `POST /api/v1/carrito/checkout`

```json
{
  "mensaje": "Compra confirmada",
  "cantidadItems": 2,
  "total": 179980
}
```

## Comunicación con otros servicios

`ms-carrito` → `ms-productos` (vía Feign, a través del API Gateway, no por IP directa):
- `GET /api/v1/productos/{id}` — validar producto
- `PUT /api/v1/productos/{id}/stock` — descontar stock al confirmar compra

## Seguridad

- CORS habilitado solo para `http://localhost:5173` (ajustar en `SecurityConfig` según el origen real del frontend desplegado).
- Todas las rutas exigen JWT válido; no hay endpoints públicos.

## Despliegue

Se despliega en una instancia EC2 junto a su propio contenedor MySQL, expuesto mediante rutas explícitas de AWS API Gateway (una por cada endpoint de la tabla anterior).
