### Universidad del Valle de Guatemala
### Fork por Hector Duarte 25939
### Original de Ing. Sebastian Arriola
para demostracion de implementacion de arbol 2-3 y tests.

# Árbol 2-3 en Java

Este proyecto es una implementación de un árbol 2-3, que es una estructura de datos que se mantiene balanceada automáticamente.

## ¿Qué hace?

Funciona como un `Map`, donde puedes:

- Agregar datos (`put`)
- Buscar datos (`get`)
- Eliminar datos (`remove`)
- Ver si una clave existe (`containsKey`)

## ¿Cómo funciona?

- Cada nodo puede tener 1 o 2 claves
- El árbol siempre se mantiene ordenado
- Todas las hojas están al mismo nivel (balanceado)

## Inserción

- Los datos se agregan en las hojas
- Si un nodo se llena, se divide (split)
- Parte de la información sube hacia arriba

## Eliminación

- Si se elimina un dato, el árbol se ajusta
- Puede tomar valores de otros nodos o juntarse (merge)

## Complejidad

Todas las operaciones son rápidas: O(log n)

## Pruebas

El código pasa todas las pruebas usando: ./gradlew test
