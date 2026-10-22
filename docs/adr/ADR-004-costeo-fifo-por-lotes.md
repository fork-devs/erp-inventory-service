# ADR-004 — Costeo FIFO por lotes y contrato completo de la saga de stock

- **Estado:** Aceptado
- **Fecha:** 2026-10-21
- **Change:** `add-inventory-service`
- **Ámbito:** `erp-inventory-service` y, por consecuencia, `erp-work-order-service`

## Contexto

`doc-microservices.md` §3.5 asigna al servicio de inventario la **táctica ACID
local + publicador de eventos**, con la decisión de que PostgreSQL es el único
escritor y de que las operaciones `Reserve`/`Commit`/`Release` son
**idempotentes**. Y §7, en el orden de desarrollo, señala el **costeo FIFO
estricto por lotes** como el *requisito central* del servicio, *"aún sin
tocar"*.

El servicio ya existía como scaffold, y su saldo era la **suma aritmética de
los movimientos**:

```java
int available = totalIn - totalOut - activeReserved;
```

Ese saldo dice **cuántas** unidades hay, y no dice **cuánto valen**. El
coste unitario no existía en el modelo: `spare_parts` no tenía columna de
coste, ni `stock_movements` la guardaba. Para un taller que compra repuestos,
el valor del stock y el coste de lo consumido son datos de negocio, no un
detalle de contabilidad: son los que permiten saber si reponer por debajo del
precio de compra, y los que.diminan el margen de la orden de trabajo.

La saga, además, estaba a medias: existían `reserve` y `release`, pero **no
existía `commit`**. Una reserva confirmada por la orden de trabajo nunca se
convertía en una salida real, y el stock quedaba **reservado para siempre**.

## Decisión

**1. El saldo se calcula con LOTES, y el consumo es FIFO.** Cada entrada de
stock abre un **lote** con su cantidad, su **coste unitario real** y su fecha
de entrada. Las salidas y las reservas **consumen primero el lote más
antiguo**, y el disponible de un repuesto es la suma del disponible de sus
lotes.

**2. La reserva se marca sobre el lote.** Un lote lleva `reserved_quantity`: su
disponible es `quantity - reserved_quantity`, y su valor reservado es
`reserved_quantity × unit_cost`. No hay una tabla de reservas aparte.

**3. Hay dos costes con papeles distintos.** `spare_parts.unit_cost` es el
**coste de referencia** del repuesto; `stock_movements.unit_cost` es el **coste
real** con el que se compró cada entrada, que puede diferir. El saldo informa
del coste real de los lotes que quedan, y el de referencia sirve para **avisar**
cuando una compra se aparta del precio esperado.

**4. La saga tiene las tres operaciones.** `ReserveStock`, `CommitStock` y
`ReleaseStock`, las tres **idempotentes** y las tres con clave de idempotencia
obligatoria.

## Motivo

**Sobre el saldo acumulado.** La alternativa obvia —un campo `stock` en la
tabla de repuestos, o una vista materializada— es más simple y es lo que hacen
la mayoría de los almacenes. Se descartó porque con un saldo acumulado el
**coste de lo consumido se pierde en el momento en que se consume**: el
inventario dice que quedan 40 unidades, pero no si son las que se compraron a
8 soles o a 80. Un saldo sin coste no puede valorarse, y el requisito de §7 no
es "cantidad disponible", sino **costeo FIFO estricto por lotes**.

**Sobre el coste medio ponderado.** Mantiene un único valor unitario y, por
eso, no necesita lotes. Se descartó porque **mezcla** el coste de unas compras
antiguas con el de unas recientes: el valor del stock deja de corresponder a
los lotes que realmente quedan. En un taller con repuestos de compra antigua y
de compra de ayer, la diferencia es real, y con el promedio el valor que sale
no es el de nada.

**Sobre FIFO y no LIFO.** LIFO dejaría los lotes antiguos sin tocar, y
congelaría su precio de compra en el inventario: el repuesto que lleva dos años
en el estante es justo el que hay que mover, y su precio de compra antiguo es
justo el que interesa conocer. FIFO consume primero lo que primero entró, de
modo que el stock que queda refleja las compras más recientes.

**Sobre marcar la reserva en el lote.** Guardar la reserva en una tabla aparte
—`reservations` con repuesto, sucursal, cantidad y estado— parece más limpio,
pero obliga a **reconstruir** qué lotes están reservados por comparación de
fechas, con ambigüedad cuando dos reservas se cruzan. Marcando la cantidad
reservada sobre el lote, el valor del stock reservado sale de la misma fila que
el disponible.

**Sobre la clave de idempotencia antes del movimiento.** El scaffold consultaba
si la clave existía y, si no, registraba el movimiento. Entre la consulta y la
escritura hay una ventana en la que otra petición con la misma clave pasa la
comprobación: dos reservas simultáneas registran **dos** movimientos, y la
segunda no ve el descuento de la primera. La clave se **reclama** ahora con una
inserción que la unicidad arbitra, y la clave y el movimiento se confirman
**juntos**: si la transacción falla, no queda una clave reclamada que bloquee
el reintento, y un fallo transitorio **no** se cachea como resultado definitivo.

**Sobre `CommitStock`.** Sin ella, la reserva y el consumo quedan en dos
operaciones distintas y, entre ellas, el stock está reservado sin estar
consumido: el balance del taller miente y la reserva vive para siempre.
Commitir con un temporizador en el inventario tampoco vale: la orden de trabajo
no sabría si la reserva se confirmó, y el temporizador sería una segunda fuente
de verdad sobre el estado de la orden.

## Alternativas descartadas

- **Saldo acumulado en la tabla de repuestos.** No permite valuar el stock.
- **Coste medio ponderado en lugar de lotes.** Mezcla costes y el valor
  resultante no corresponde a ningún lote real.
- **Reservas en una tabla aparte.** Obliga a reconstruir el reparto por lotes.
- **La reserva solo como cantidad global.** No se puede saber cuánto stock
  reservado hay ni cuánto vale.
- **Que el inventario confirme la reserva solo, con un temporizador.** Segunda
  fuente de verdad sobre el estado de la orden de trabajo.
- **Publicar los eventos sin bandeja de salida.** El bus puede recibir un
  evento que la base de datos no llegó a confirmar, y un fallo del bus pierde el
  evento sin revertir el stock. Es el defecto que `fix-transactional-outbox`
  cerró en el tenant-service y el hub.
- **Esperar a Kafka para construir la bandeja.** El transporte es lo único que
  depende del bus: la bandeja no.

## Consecuencias

**Positivas.**

- El taller sabe **cuánto vale** su stock y **cuánto le cuesta** lo que consume.
- La compra que se aparta del precio de referencia se puede **detectar**, en
  lugar de quedar diluida en un promedio.
- La saga queda **completa** y el stock no queda reservado para siempre.
- Dos reservas simultáneas no pueden **sobre-reservar**, y dos peticiones con
  la misma clave no pueden registrar dos movimientos.
- El stock **avisa** cuando un repuesto cae por debajo de su umbral.

**Negativas.**

- El modelo **crece**: una tabla de lotes, una columna de coste en el repuesto y
  otra en el movimiento. Es el precio del requisito de §7, y el modelo queda
  documentado en `docs/doc-database-diagrams.md` §5.
- El **costeo FIFO** exige bloquear los lotes que se consumen, en orden de
  entrada. Dos reservas concurrentes del mismo repuesto se serializan; las de
  otros repuestos, no.
- `CommitStock` es un cambio **incompatible** del contrato de la saga: quien
  la invoque tiene que conocerla. Por eso este ADR existe **antes** de
  construir `erp-work-order-service`.

**Neutrales.**

- El cambio se aplica sobre un servicio **sin desplegar con datos reales**: la
  tabla de movimientos está vacía, de modo que la migración de lotes no tiene
  que backfill. Es la ventana más barata para cambiar el modelo.
- El transporte de los eventos sigue siendo el `EventPublisherPort`: **Kafka
  todavía no existe** en la plataforma, y la bandeja no depende del bus.

## Alcance de la decisión posterior

- **Quién invoca `CommitStock`** lo decide `erp-work-order-service`, cuyo change
  tendrá que declarar el contrato de la saga con las tres operaciones. Este
  ADR fija el contrato del lado del inventario, no el del orquestador.
- **El umbral de desviación de coste** es un valor global configurable por
  entorno. Si los talleres necesitan umbrales distintos, hará falta una tabla
  de preferencias: es una decisión posterior y no cambia el comportamiento
  observable del saldo.
- **El transporte a Kafka** es un change aparte, con la bandeja ya construida.
