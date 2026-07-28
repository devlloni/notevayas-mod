# NoTeVayas

Mod de Fabric para **Minecraft 26.2** con un sistema de cultivo botánico completo:
4 cepas, cultivo indoor y outdoor, curado, calidad, consumibles con efectos y
generación en el mundo.

Incluye además el comando `/notevayas`, que reproduce un sonido en la posición del
jugador que lo ejecuta.

## Requisitos

| | |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | >= 0.19.3 |
| Fabric API | requerida |
| Java | 25 |

## Instalación

El mod va **tanto en el servidor como en cada cliente**. El servidor corre la lógica,
pero las texturas y el audio viajan dentro del mod, así que un cliente sin instalarlo
no tiene con qué renderizar ni reproducir nada.

Copiá `notevayas-<version>.jar` a la carpeta `mods/`.

---

## Las 4 cepas

| Cepa | Tipo | Efectos | Contra |
|---|---|---|---|
| **Piedra Roja** | indica | Resistencia II, Regeneración I, Absorción I | Lentitud I |
| **Verde Sol** | sativa | Velocidad II, Prisa I, Impulso de Salto I | Náusea 5s |
| **Media Luna** | híbrida | Resistencia I, Velocidad I, Regeneración I | — (dura 40% menos) |
| **Púrpura Nocturna** | nocturna | Visión Nocturna, Caída Lenta, Absorción II | Debilidad I |

## Cultivo

Las semillas se plantan sobre **farmland** y pasan por 8 etapas de crecimiento.

**Luz:** Piedra Roja, Verde Sol y Media Luna necesitan luz **≥ 9**. Púrpura Nocturna
necesita luz **≤ 7** — crece de noche o en interiores a oscuras.

> A diferencia del trigo vanilla, el mod mide la luz **real** (según la hora del día).
> Eso significa que las cepas diurnas **frenan de noche a cielo abierto**, salvo que
> tengan una lámpara. Es lo que le da sentido al indoor.

**Bonus de velocidad:**

- Verde Sol crece **1.5×** en Savanna, Plains y Desert
- Si llueve y la planta ve el cielo: **+25%** de probabilidad de crecer
- Con lámpara de cultivo prendida arriba: **2×** y se ignora el requisito de luz

**Al romper el cultivo:**

- Maduro (etapa 7) → 1 cogollo fresco + 1-3 semillas
- Inmaduro → 1 semilla, sin cogollo

## Indoor

**Lámpara de Cultivo** — se prende con redstone y emite luz nivel 15. Si hay una
prendida hasta **3 bloques por encima** de un cultivo, la planta ignora el requisito de
luz natural y crece al doble.

**Maceta Hidropónica** — puesta **directamente debajo del farmland** (dos bloques bajo
la planta), suma +1 de calidad al cosechar y evita que el farmland se seque nunca.

## Calidad

Los cogollos llevan un nivel de calidad de 0 a 3, visible en el tooltip. Dos stacks de
distinta calidad **no se apilan**.

| Nivel | Nombre | Color | Duración de efectos |
|---|---|---|---|
| 0 | Prensado | gris | 0.5× |
| 1 | Regular | blanco | 1.0× |
| 2 | Premium | verde | 1.5× |
| 3 | Cogollo Élite | dorado | 2.0× **y +1 al amplificador** |

Se calcula al cosechar, empezando en 0:

```
+1  si hubo una lámpara de cultivo activa
+1  si hay una maceta hidropónica debajo
+1  si el bioma le queda bien a la cepa
+1  si llovió durante el crecimiento
-1  con 30% de probabilidad, si creció a la intemperie y sin ninguna asistencia
```

## Curado

El **Secadero** tiene 4 slots y convierte cogollo fresco en cogollo seco en **4 minutos**
por slot, conservando la calidad.

- Click derecho **con cogollo fresco** → lo mete
- Click derecho **con la mano vacía** → saca todo lo que ya esté listo

El progreso se guarda al descargar el chunk.

> Comerse un cogollo **fresco** sin curar da los efectos al **30%** de duración más
> Náusea 10s garantizada. Vale la pena esperar.

## Consumibles

| Item | Duración base | Extra |
|---|---|---|
| Porro | 45s | — |
| Blunt | 120s | — |
| Bong *(bloque)* | 25s | +1 amplificador, Náusea 3s al inicio |
| Brownie | 90s | **onset retardado de 15s** |
| Space Cake | 240s | **onset retardado de 30s**, +1 amplificador |

El onset retardado usa un efecto de estado propio (`digestion`) que al expirar dispara
los efectos reales — sin schedulers globales.

**Munchies** — acompaña a cualquier consumible, con la misma duración. Mientras está
activo el hambre baja **50% más rápido**, pero cualquier comida da **+50% de saturación**.

**Bong** — necesita agua. Se carga con un balde (4 usos) y se usa con click derecho
sobre un cogollo seco. Sin agua avisa en la action bar.

## Recetas

| Resultado | Ingredientes |
|---|---|
| 4 × Papel | 1 papel + 1 caña de azúcar |
| 1 × Moledor | 5 lingotes de hierro + 1 redstone |
| 2 × Porro | 1 cogollo seco + 1 papel |
| 1 × Blunt | 2 cogollos secos + 1 papel + 1 azúcar |
| 3 × Brownie | 1 cogollo seco + 1 cacao + 1 trigo + 1 huevo |
| 1 × Space Cake | 1 torta + 3 cogollos secos |
| Lámpara de Cultivo | 4 hierro + 1 glowstone + 2 redstone + 1 lámpara de redstone |
| Maceta Hidropónica | 5 lingotes de cobre + 1 balde de agua + 1 bloque de tierra |
| Secadero | 6 palos + 2 cuerdas |
| Bong | 3 vidrio + 2 lingotes de cobre + 1 balde de agua |

**El Moledor**, si está en la grilla al armar porros, sube el rendimiento a **4** en vez
de 2 y se queda en la grilla gastando 1 de durabilidad (64 usos).

Los consumibles **heredan la cepa y la calidad** del cogollo usado. Si se mezclan
cogollos de distinta calidad, se usa la **menor**.

Los consumibles son una receta especial (`CustomRecipe`) porque tienen que copiar
components y aplicar la regla del moledor, cosas que un JSON no puede hacer. Igual
aparecen en el libro de recetas: la receta expone un `display()` por combinación, con los
cogollos mostrados como el tag `#notevayas:cogollos` (el libro cicla las 4 cepas).

Todas las recetas del mod traen su advancement en `data/notevayas/advancement/recipes/`.
Sin eso el libro nunca las desbloquea, aunque estén cargadas.

## Cómo se consiguen las primeras semillas

Dos vías, las dos dan una cepa al azar (Púrpura Nocturna con la mitad de probabilidad):

1. **Cortando pasto** — 3% por mata de pasto, helecho o pasto alto. Es la vía que
   funciona en un mundo ya explorado.
2. **Cáñamo Silvestre** — la planta generada en el mundo, ver abajo. Solo aparece en
   chunks nuevos.

## Generación en el mundo

**Cáñamo Silvestre** aparece en Plains, Sunflower Plains, Savanna, Savanna Plateau,
Forest, Flower Forest, Birch Forest, Taiga y Meadow. Al romperlo dropea 1-2 semillas de
una cepa al azar; Púrpura Nocturna tiene la mitad de probabilidad que las demás.

No tiene item propio: se obtiene rompiéndolo. Para verlo en creativo:

```
/setblock ~ ~ ~ notevayas:canamo_silvestre
```

---

## Dónde tocar los números

| Qué | Archivo |
|---|---|
| Duraciones de los consumibles | `PerfilConsumo.java` |
| Efectos de cada cepa | `EfectosCepa.java` |
| Penalización de Media Luna | `EfectosCepa.java` |
| Multiplicadores de calidad | `Calidad.java` |
| Fórmula de calidad al cosechar | `CultivoBlock.calcularCalidad` |
| Luz, bioma, lluvia, lámpara | constantes al principio de `CultivoBlock.java` |
| Biomas favorables por cepa | `Cepa.biomasFavorables` |
| Tiempo de curado | `SecaderoBlockEntity.TICKS_DE_CURADO` |
| Munchies (hambre y saturación) | `ModEffects.java` |
| Usos de agua del bong | `BongBlock.USOS_POR_BALDE` |
| Rareza del cáñamo silvestre | `data/notevayas/worldgen/placed_feature/canamo_silvestre.json` |
| Probabilidad de semilla en el pasto | `ModLoot.PROBABILIDAD` |
| Drops del cultivo | `data/notevayas/loot_table/blocks/cultivo_*.json` |

## Compilar

```bash
./gradlew build
```

El jar queda en `build/libs/`. Ignorá el `-sources.jar`.

Para probar en vivo:

```bash
./gradlew runClient
```

## Pendiente

- Faltan las texturas de los íconos de efecto (18×18):
  `assets/notevayas/textures/mob_effect/munchies.png` y `digestion.png`.
  Los efectos funcionan, pero el ícono del HUD sale como textura faltante.
- El Cáñamo Silvestre reutiliza la textura `cultivo_stage3` por ahora.
- Los biomas favorables de Piedra Roja, Media Luna y Púrpura Nocturna son provisorios
  (están marcados con un comentario en `Cepa.java`).

## Licencia

MIT. Ver [LICENSE](LICENSE).
